package com.alexandre.solicitations.solicitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alexandre.solicitations.auth.AuthenticatedUser;
import com.alexandre.solicitations.shared.BrazilianState;
import com.alexandre.solicitations.shared.exception.ForbiddenException;
import com.alexandre.solicitations.shared.exception.NotFoundException;
import com.alexandre.solicitations.solicitation.dto.DecisionRequest;
import com.alexandre.solicitations.solicitation.search.SolicitationSearchService;
import com.alexandre.solicitations.user.Role;
import com.alexandre.solicitations.user.UserService;
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
