package com.alexandre.solicitations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.alexandre.solicitations.audit.AuditLog;
import com.alexandre.solicitations.audit.AuditLogRepository;
import com.alexandre.solicitations.cep.CepAddress;
import com.alexandre.solicitations.cep.ViaCepClient;
import com.alexandre.solicitations.shared.BrazilianState;
import com.alexandre.solicitations.solicitation.search.SolicitationDocument;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Real PostgreSQL and Elasticsearch (Testcontainers); only ViaCEP is mocked. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SolicitationFlowIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    ElasticsearchOperations elasticsearch;

    @Autowired
    AuditLogRepository auditLogs;

    @MockitoBean
    ViaCepClient viaCepClient;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void securityErrorsAreProblemDetails() throws Exception {
        mvc.perform(post("/solicitations"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        String client = registerAndLogin("client-security@test.dev");
        mvc.perform(auth(get("/analyst/solicitations/search"), client))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void validationErrorsListTheFields() throws Exception {
        String client = registerAndLogin("client-validation@test.dev");
        String id = createDraft(client);

        mvc.perform(auth(put("/solicitations/{id}/basic-info", id), client)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceType": "INSTALLATION", "title": "  ab  ", "description": "too short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.description").exists());
    }

    @Test
    void fullFlowFromDraftToDecision() throws Exception {
        when(viaCepClient.lookup("01310100"))
                .thenReturn(
                        new CepAddress("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", BrazilianState.SP));

        String admin = login("admin@test.dev", "admin12345");
        String analystSp = createAnalyst(admin, "analyst-sp@test.dev", "SP");
        String analystRj = createAnalyst(admin, "analyst-rj@test.dev", "RJ");
        String client = registerAndLogin("client-flow@test.dev");

        // Steps 1 and 2, then an early submit lists what's missing
        String id = createDraft(client);
        mvc.perform(auth(put("/solicitations/{id}/basic-info", id), client)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceType": "INSTALLATION", "title": "Instalação de ar condicionado",
                                 "description": "Instalar dois aparelhos split na sala comercial"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(1));
        mvc.perform(auth(put("/solicitations/{id}/address", id), client)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cep": "01310-100", "number": "1000A"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(2))
                .andExpect(jsonPath("$.address.state").value("SP"));

        mvc.perform(auth(post("/solicitations/{id}/submit", id), client))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.missingSteps[0]").value("confirmation"));

        // Step 3 and submit
        mvc.perform(auth(put("/solicitations/{id}/confirmation", id), client)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"priority": "HIGH", "preferredDate": "%s", "estimatedValue": 1500.00,
                                 "termsAccepted": true}
                                """.formatted(LocalDate.now().plusDays(7))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(3));
        mvc.perform(auth(post("/solicitations/{id}/submit", id), client))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        elasticsearch.indexOps(SolicitationDocument.class).refresh();

        // An analyst from another UF can't see it, not even asking for state=SP
        mvc.perform(auth(get("/analyst/solicitations/{id}", id), analystRj)).andExpect(status().isForbidden());
        mvc.perform(auth(get("/analyst/solicitations/search").param("state", "SP"), analystRj))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        // The SP analyst finds it by text, without accents and with a typo, and decides
        mvc.perform(auth(get("/analyst/solicitations/search").param("q", "instalacao"), analystSp))
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(auth(get("/analyst/solicitations/search").param("q", "condicinado"), analystSp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
        mvc.perform(auth(post("/analyst/solicitations/{id}/start", id), analystSp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
        mvc.perform(auth(post("/analyst/solicitations/{id}/decide", id), analystSp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision": "APPROVE", "comment": "Documentação conferida"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertThat(auditLogs.findByResourceIdOrderByIdAsc(UUID.fromString(id)))
                .extracting(AuditLog::getAction)
                .containsExactly("SOLICITATION_SUBMITTED", "REVIEW_STARTED", "SOLICITATION_DECIDED");
    }

    private String createDraft(String token) throws Exception {
        ResultActions result = mvc.perform(auth(post("/solicitations"), token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentStep").value(0));
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String createAnalyst(String adminToken, String email, String state) throws Exception {
        mvc.perform(auth(post("/admin/users"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Analyst", "email": "%s", "password": "secret123",
                                 "role": "ANALYST", "states": ["%s"]}
                                """.formatted(email, state)))
                .andExpect(status().isCreated());
        return login(email, "secret123");
    }

    private String registerAndLogin(String email) throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Client", "email": "%s", "password": "secret123"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
        return login(email, "secret123");
    }

    private String login(String email, String password) throws Exception {
        ResultActions result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk());
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.accessToken");
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }
}
