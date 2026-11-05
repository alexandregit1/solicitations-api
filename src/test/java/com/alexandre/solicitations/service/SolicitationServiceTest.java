package com.alexandre.solicitations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alexandre.solicitations.client.ViaCepClient;
import com.alexandre.solicitations.dto.AddressRequest;
import com.alexandre.solicitations.dto.CepAddress;
import com.alexandre.solicitations.exception.BusinessException;
import com.alexandre.solicitations.exception.ForbiddenException;
import com.alexandre.solicitations.exception.IncompleteSolicitationException;
import com.alexandre.solicitations.model.Solicitation;
import com.alexandre.solicitations.model.Solicitations;
import com.alexandre.solicitations.model.enums.BrazilianState;
import com.alexandre.solicitations.model.enums.ServiceType;
import com.alexandre.solicitations.model.enums.SolicitationStatus;
import com.alexandre.solicitations.repository.SolicitationRepository;
import jakarta.validation.Validation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

class SolicitationServiceTest {

    private final SolicitationRepository repository = mock();
    private final ViaCepClient viaCepClient = mock();
    private final SolicitationService service = new SolicitationService(
            repository,
            viaCepClient,
            mock(SolicitationSearchService.class),
            Validation.buildDefaultValidatorFactory().getValidator());

    private final UUID clientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void onlyTheOwnerReachesASolicitation() {
        Solicitation solicitation = stored(Solicitations.draft(clientId));

        assertThatThrownBy(() -> service.get(solicitation.getId(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.submit(solicitation.getId(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void addressIsCompletedFromViaCep() {
        Solicitation solicitation = Solicitations.draft(clientId);
        solicitation.updateBasicInfo(ServiceType.INSTALLATION, "Air conditioning", "Install two split units");
        stored(solicitation);
        when(viaCepClient.lookup("01310100"))
                .thenReturn(
                        new CepAddress("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", BrazilianState.SP));

        var response =
                service.updateAddress(solicitation.getId(), clientId, new AddressRequest("01310-100", "12A", null));

        assertThat(response.currentStep()).isEqualTo(2);
        assertThat(response.address().city()).isEqualTo("São Paulo");
        assertThat(response.address().number()).isEqualTo("12A");
    }

    @Test
    void invalidCepDoesNotCompleteTheStep() {
        Solicitation solicitation = Solicitations.draft(clientId);
        solicitation.updateBasicInfo(ServiceType.INSTALLATION, "Air conditioning", "Install two split units");
        stored(solicitation);
        when(viaCepClient.lookup("99999999")).thenThrow(new BusinessException("CEP not found"));

        assertThatThrownBy(() -> service.updateAddress(
                        solicitation.getId(), clientId, new AddressRequest("99999-999", "1", null)))
                .isInstanceOf(BusinessException.class);
        assertThat(solicitation.getCurrentStep()).isEqualTo(1);
    }

    @Test
    void incompleteSubmitListsTheMissingStepsAndKeepsTheDraft() {
        Solicitation solicitation = Solicitations.draft(clientId);
        solicitation.updateBasicInfo(ServiceType.INSTALLATION, "Air conditioning", "Install two split units");
        stored(solicitation);

        assertThatThrownBy(() -> service.submit(solicitation.getId(), clientId))
                .isInstanceOfSatisfying(IncompleteSolicitationException.class, ex -> {
                    ProblemDetail problem = ex.getBody();
                    assertThat(problem.getProperties())
                            .containsEntry("missingSteps", List.of("address", "confirmation"))
                            .containsEntry("errors", Map.of());
                });
        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.DRAFT);
    }

    @Test
    void completeSubmitSendsForReview() {
        Solicitation solicitation = stored(Solicitations.complete(clientId, BrazilianState.SP));

        var response = service.submit(solicitation.getId(), clientId);

        assertThat(response.status()).isEqualTo(SolicitationStatus.SUBMITTED);
        assertThat(response.submittedAt()).isNotNull();
    }

    // The entity gets its id from JPA; here a mock repository stands in, so the lookup is by any id
    private Solicitation stored(Solicitation solicitation) {
        when(repository.findById(any())).thenReturn(Optional.of(solicitation));
        return solicitation;
    }
}
