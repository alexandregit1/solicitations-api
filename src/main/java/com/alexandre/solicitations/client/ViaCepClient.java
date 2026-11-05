package com.alexandre.solicitations.client;

import com.alexandre.solicitations.dto.CepAddress;
import com.alexandre.solicitations.exception.BusinessException;
import com.alexandre.solicitations.model.enums.BrazilianState;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/** Looks up an address on ViaCEP (https://viacep.com.br). */
@Slf4j
@Component
public class ViaCepClient {

    private final RestClient restClient;

    public ViaCepClient(RestClient.Builder builder, @Value("${viacep.url}") String baseUrl) {
        // Short timeouts: a slow ViaCEP must not hold the request
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient =
                builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /**
     * @param cep 8 digits
     * @throws BusinessException (422) if the CEP doesn't exist
     * @throws ResponseStatusException (503) if ViaCEP fails or times out
     */
    public CepAddress lookup(String cep) {
        ViaCepResponse response;
        try {
            response = restClient.get().uri("/{cep}/json/", cep).retrieve().body(ViaCepResponse.class);
        } catch (RestClientException ex) {
            log.warn("ViaCEP lookup failed for {}", cep, ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "CEP service unavailable, try again");
        }

        // ViaCEP answers 200 with {"erro": true} for a CEP that doesn't exist
        if (response == null || response.uf() == null) {
            throw new BusinessException("CEP not found");
        }
        return new CepAddress(
                cep,
                response.logradouro(),
                response.bairro(),
                response.localidade(),
                BrazilianState.valueOf(response.uf()));
    }

    record ViaCepResponse(String logradouro, String bairro, String localidade, String uf) {}
}
