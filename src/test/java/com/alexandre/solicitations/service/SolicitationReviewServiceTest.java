package com.alexandre.solicitations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alexandre.solicitations.dto.DecisionRequest;
import com.alexandre.solicitations.exception.ForbiddenException;
import com.alexandre.solicitations.exception.NotFoundException;
import com.alexandre.solicitations.model.Solicitation;
import com.alexandre.solicitations.model.Solicitations;
import com.alexandre.solicitations.model.enums.BrazilianState;
import com.alexandre.solicitations.model.enums.Decision;
import com.alexandre.solicitations.model.enums.Role;
import com.alexandre.solicitations.model.enums.SolicitationStatus;
import com.alexandre.solicitations.repository.SolicitationRepository;
import com.alexandre.solicitations.security.AuthenticatedUser;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SolicitationReviewServiceTest {

    private final SolicitationRepository repository = mock();
    private final UserService userService = mock();
    private final SolicitationReviewService service =
            new SolicitationReviewService(repository, userService, mock(SolicitationSearchService.class));

    private final AuthenticatedUser analyst = new AuthenticatedUser(UUID.randomUUID(), Role.ANALYST);
    private final AuthenticatedUser admin = new AuthenticatedUser(UUID.randomUUID(), Role.ADMIN);
    private final DecisionRequest approve = new DecisionRequest(Decision.APPROVE, "All documents checked");

    @BeforeEach
    void setUp() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userService.getStates(analyst.id())).thenReturn(Set.of(BrazilianState.SP, BrazilianState.MG));
    }

    @Test
    void analystDecidesInsideTheirCoverage() {
        stored(Solicitations.submitted(UUID.randomUUID(), BrazilianState.SP));

        assertThat(service.decide(UUID.randomUUID(), analyst, approve).status()).isEqualTo(SolicitationStatus.APPROVED);
    }

    @Test
    void analystCannotReachAnotherUf() {
        stored(Solicitations.submitted(UUID.randomUUID(), BrazilianState.RJ));

        assertThatThrownBy(() -> service.get(UUID.randomUUID(), analyst)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.decide(UUID.randomUUID(), analyst, approve))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void adminReachesEveryUf() {
        stored(Solicitations.submitted(UUID.randomUUID(), BrazilianState.RJ));

        assertThat(service.start(UUID.randomUUID(), admin).status()).isEqualTo(SolicitationStatus.IN_REVIEW);
    }

    @Test
    void draftsDoNotExistForReviewers() {
        stored(Solicitations.complete(UUID.randomUUID(), BrazilianState.SP));

        assertThatThrownBy(() -> service.get(UUID.randomUUID(), admin)).isInstanceOf(NotFoundException.class);
    }

    private void stored(Solicitation solicitation) {
        when(repository.findById(any())).thenReturn(Optional.of(solicitation));
    }
}
