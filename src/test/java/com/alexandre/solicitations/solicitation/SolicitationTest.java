package com.alexandre.solicitations.solicitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.alexandre.solicitations.shared.BrazilianState;
import com.alexandre.solicitations.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitationTest {

    private final UUID clientId = UUID.randomUUID();
    private final UUID analystId = UUID.randomUUID();

    @Test
    void startsAsDraftWithNoCompletedStep() {
        Solicitation solicitation = Solicitations.draft(clientId);

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(solicitation.getCurrentStep()).isZero();
    }

    @Test
    void currentStepAdvancesAsStepsAreCompleted() {
        assertThat(Solicitations.complete(clientId, BrazilianState.SP).getCurrentStep())
                .isEqualTo(3);
    }

    @Test
    void cannotSkipAStep() {
        Solicitation solicitation = Solicitations.draft(clientId);

        assertThatThrownBy(() -> solicitation.updateConfirmation(Priority.LOW, LocalDate.now(), BigDecimal.ONE, true))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Complete step 1 first");
    }

    @Test
    void savingAnEarlierStepAgainKeepsProgress() {
        Solicitation solicitation = Solicitations.complete(clientId, BrazilianState.SP);

        solicitation.updateBasicInfo(ServiceType.INSPECTION, "New title", "A new description long enough");

        assertThat(solicitation.getCurrentStep()).isEqualTo(3);
        assertThat(solicitation.getServiceType()).isEqualTo(ServiceType.INSPECTION);
    }

    @Test
    void onlyADraftCanBeEdited() {
        Solicitation solicitation = Solicitations.submitted(clientId, BrazilianState.SP);

        assertThatThrownBy(() -> solicitation.updateBasicInfo(ServiceType.INSPECTION, "Title", "Description"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reviewFlow() {
        Solicitation solicitation = Solicitations.submitted(clientId, BrazilianState.SP);

        solicitation.startReview(analystId);
        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.IN_REVIEW);

        solicitation.decide(Decision.APPROVE, "All documents checked", analystId);
        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.APPROVED);
        assertThat(solicitation.getDecidedAt()).isNotNull();
    }

    @Test
    void submittedCanBeDecidedWithoutStartingReview() {
        Solicitation solicitation = Solicitations.submitted(clientId, BrazilianState.SP);

        solicitation.decide(Decision.REJECT, "Outside the service area", analystId);

        assertThat(solicitation.getStatus()).isEqualTo(SolicitationStatus.REJECTED);
    }

    @Test
    void cannotDecideADraftOrDecideTwice() {
        Solicitation draft = Solicitations.draft(clientId);
        assertThatThrownBy(() -> draft.decide(Decision.APPROVE, "comment", analystId))
                .isInstanceOf(ConflictException.class);

        Solicitation decided = Solicitations.submitted(clientId, BrazilianState.SP);
        decided.decide(Decision.APPROVE, "comment", analystId);
        assertThatThrownBy(() -> decided.decide(Decision.REJECT, "comment", analystId))
                .isInstanceOf(ConflictException.class);
    }
}
