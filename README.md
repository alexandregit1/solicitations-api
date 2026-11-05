# Solicitations API

API REST de **Solicitações de Atendimento**. O cliente preenche uma solicitação em 3 etapas, podendo salvar o rascunho e continuar depois. Depois de enviada, um analista que cobre a UF do endereço analisa e decide.

**Stack:** Java 21 · Spring Boot 4 · PostgreSQL 17 · Elasticsearch 9 · Flyway · JWT · Docker

## Como rodar

```bash
docker compose up --build
```

Sobe a API, o PostgreSQL e o Elasticsearch, sem precisar de `.env`.

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html (botão **Authorize**: cole o `accessToken` do login)
- ADMIN inicial: `admin@local.dev` / `admin12345`

Porta ocupada? Troque por variável: `SERVER_PORT=18080 POSTGRES_PORT=15432 docker compose up --build`.

**Pela IDE:** suba só a infraestrutura (`docker compose up postgres elasticsearch`), crie o `.env` a partir do `.env.example` e carregue-o na run configuration.

## Perfis

| Perfil | Como nasce | O que faz |
|---|---|---|
| `CLIENT` | `POST /auth/register` | Cria e envia as próprias solicitações |
| `ANALYST` | Criado pelo ADMIN, com as UFs que cobre | Analisa e decide solicitações das suas UFs |
| `ADMIN` | Criado pelo ADMIN; o primeiro vem de `ADMIN_EMAIL` | Cria usuários internos e acessa as rotas de analista em qualquer UF |

## Etapas

`POST /solicitations` cria um rascunho (`DRAFT`) vazio. Cada etapa é salva separadamente, e `GET /solicitations/{id}` devolve o que já foi salvo.

| Etapa | Rota | Regras |
|---|---|---|
| 1. Dados básicos | `PUT /solicitations/{id}/basic-info` | `serviceType`: INSTALLATION, MAINTENANCE ou INSPECTION. `title`: 3 a 80 caracteres. `description`: 20 a 1000. Medidos depois do trim. |
| 2. Endereço | `PUT /solicitations/{id}/address` | `cep` (`00000-000` ou `00000000`) consultado no ViaCEP, que preenche rua, bairro, cidade e UF. `number`: 1 a 20 caracteres (aceita "12A"). `complement` opcional. CEP inexistente (422) ou ViaCEP fora (503): a etapa não conclui. |
| 3. Confirmação | `PUT /solicitations/{id}/confirmation` | `priority`: LOW, MEDIUM ou HIGH. `preferredDate`: hoje ou depois. `estimatedValue` ≥ 0, e ≥ 100 se HIGH. `termsAccepted` = true. |

- `currentStep` é o número de etapas concluídas (0 a 3). Uma etapa só pode ser salva depois das anteriores.
- Só um `DRAFT` pode ser editado.
- `POST /solicitations/{id}/submit` revalida todas as etapas. Se faltar algo, responde 422 com o que falta:

```json
{
  "status": 422,
  "detail": "The solicitation is incomplete and cannot be submitted",
  "missingSteps": ["confirmation"],
  "errors": { "basicInfo.title": "size must be between 3 and 80" }
}
```

## Status

```
DRAFT → SUBMITTED → IN_REVIEW → APPROVED / REJECTED
```

`IN_REVIEW` é opcional: `decide` aceita uma solicitação `SUBMITTED` ou `IN_REVIEW`.

## Endpoints

| Endpoint | Perfil | Regra |
|---|---|---|
| `POST /auth/register` | público | Cria apenas CLIENT |
| `POST /auth/login` | público | Devolve o `accessToken` (JWT) |
| `POST /admin/users` | ADMIN | Cria ANALYST (com `states`) ou ADMIN |
| `POST /solicitations` | CLIENT | Cria um rascunho |
| `GET /solicitations/{id}` | CLIENT dono | Para continuar de onde parou |
| `PUT /solicitations/{id}/basic-info`, `/address`, `/confirmation` | CLIENT dono | Salva uma etapa |
| `POST /solicitations/{id}/submit` | CLIENT dono | Só em DRAFT, com validação completa |
| `GET /analyst/solicitations/{id}` | ANALYST da UF, ADMIN | Rascunhos não aparecem (404) |
| `POST /analyst/solicitations/{id}/start` | ANALYST da UF, ADMIN | SUBMITTED → IN_REVIEW |
| `POST /analyst/solicitations/{id}/decide` | ANALYST da UF, ADMIN | `decision`: APPROVE ou REJECT. `comment`: 10 a 1000 caracteres |
| `GET /analyst/solicitations/search` | ANALYST, ADMIN | Busca no Elasticsearch |

## Busca

Todos os parâmetros são opcionais.

| Parâmetro | Efeito |
|---|---|
| `q` | Texto no título (peso maior), descrição, bairro e cidade. Analyzer em português (ignora acentos e plurais) e tolerância a erros de digitação. |
| `status`, `serviceType`, `priority`, `state` | Filtros exatos |
| `dateFrom`, `dateTo` | Data de criação, `yyyy-MM-dd`, inclusivos |
| `page`, `size` | Padrão 0 e 20; `size` até 100 |
| `sort` | `createdAt`, `preferredDate` ou `estimatedValue`, com `,asc` ou `,desc`. Padrão: relevância com `q`, senão `createdAt,desc`. |

Resposta: `{ "items": [...], "page": 0, "size": 20, "total": 1 }`.

**Filtro de UF forçado:** para o ANALYST, a busca sempre fica limitada às UFs que ele cobre. Um `state` fora da cobertura não amplia nada: devolve lista vazia.

As solicitações são indexadas ao criar, ao salvar cada etapa, no submit, no início da análise e na decisão.

## Erros

Todos os erros saem como `ProblemDetail` (RFC 9457), inclusive 401 e 403. Erros de validação trazem `errors: { campo: mensagem }`.

| Status | Quando |
|---|---|
| 400 | Payload ou parâmetro inválido |
| 401 | Sem token, token inválido ou expirado, login errado |
| 403 | Perfil sem acesso, solicitação de outro cliente ou fora da cobertura de UF |
| 404 | Não existe |
| 409 | Status não permite a operação, etapa fora de ordem, e-mail já cadastrado, edição simultânea |
| 422 | Regra de negócio: CEP inexistente, envio incompleto, perfil inválido |
| 503 | ViaCEP indisponível |

## Auditoria

Os métodos marcados com `@Audit` gravam em `audit_logs` a ação, quem fez e a solicitação: `SOLICITATION_SUBMITTED`, `REVIEW_STARTED` e `SOLICITATION_DECIDED`. Cada registro também incrementa a métrica `audit.actions` (Micrometer), disponível para o ADMIN em `/actuator/metrics/audit.actions`.

## Decisões

- **Pacotes por camada** (`controller`, `service`, `repository`, `model`, `dto`, `exception`, `config`, `security`, `client`, `aspect`). A API recebe e devolve DTOs, nunca entidades.
- **Regras no lugar certo:** os campos de cada etapa são validados nos DTOs (Bean Validation). A ordem das etapas e o fluxo de status ficam na entidade `Solicitation`. Dono e cobertura de UF ficam nos services.
- **O submit reaproveita as validações das etapas:** remonta cada DTO a partir dos dados salvos e valida de novo. Assim, nenhuma regra é duplicada.
- **A cobertura de UF é lida do banco a cada requisição, não do token.** Uma mudança vale na hora.
- **O PostgreSQL é a fonte da verdade e o Elasticsearch é uma cópia para busca.** Uma falha de indexação é logada e não derruba a requisição.
- **As datas de calendário** (`preferredDate`, `dateFrom`/`dateTo`) seguem o fuso de Brasília (`TZ` no compose).

## Testes

```bash
./mvnw verify
```

Só precisa do Docker rodando: o Testcontainers sobe um PostgreSQL e um Elasticsearch temporários. O `verify` também confere a formatação (Spotless); para formatar, rode `./mvnw spotless:apply`.

- **Unitários:** regras da entidade, validação das etapas, dono e envio, cobertura de UF e o filtro forçado da busca.
- **Integração** (`SolicitationFlowIntegrationTest`): o fluxo completo pela API, do registro à decisão, incluindo busca e `audit_logs`. Só o ViaCEP é mockado.

## Configuração

Tudo vem de variáveis de ambiente. O `compose.yaml` tem valores padrão e o `.env.example` lista todas.

| Variável | Uso |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL |
| `ELASTICSEARCH_URIS` | Elasticsearch |
| `JWT_SECRET`, `JWT_EXPIRATION` | Chave HS256 (pelo menos 32 caracteres) e validade do token |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Primeiro ADMIN, criado na subida se não existir nenhum |
| `VIACEP_URL` | Base do ViaCEP |
| `SERVER_PORT`, `SWAGGER_ENABLED`, `LOG_LEVEL` | Porta, Swagger e nível de log |
| `POSTGRES_PORT`, `ELASTICSEARCH_PORT` | Só no compose: portas publicadas no host |
