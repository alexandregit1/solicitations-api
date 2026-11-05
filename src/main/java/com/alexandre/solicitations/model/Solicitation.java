package com.alexandre.solicitations.model;

import com.alexandre.solicitations.exception.ConflictException;
import com.alexandre.solicitations.model.enums.Decision;
import com.alexandre.solicitations.model.enums.Priority;
import com.alexandre.solicitations.model.enums.ServiceType;
import com.alexandre.solicitations.model.enums.SolicitationStatus;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Filled in 3 steps while DRAFT, then reviewed. Field rules live in the request DTOs;
 * this class guards the step order and the status flow.
 */
@Entity
@Table(name = "solicitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Solicitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID clientId;

    @Enumerated(EnumType.STRING)
    private SolicitationStatus status;

    // Completed steps, 0 to 3
    private int currentStep;

    // Step 1: basic info
    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    private String title;
    private String description;

    // Step 2: address
    @Embedded
    private Address address;

    // Step 3: confirmation
    @Enumerated(EnumType.STRING)
    private Priority priority;

    private LocalDate preferredDate;
    private BigDecimal estimatedValue;
    private Boolean termsAccepted;

    // Review
    private UUID analystId;
    private String decisionComment;

    private Instant createdAt;
    private Instant submittedAt;
    private Instant decidedAt;

    // Optimistic lock: two simultaneous edits don't overwrite each other
    @Version
    private Long version;

    public Solicitation(UUID clientId) {
        this.clientId = clientId;
        this.status = SolicitationStatus.DRAFT;
        this.createdAt = Instant.now();
    }

    public boolean belongsTo(UUID userId) {
        return clientId.equals(userId);
    }

    public boolean isDraft() {
        return status == SolicitationStatus.DRAFT;
    }

    public void updateBasicInfo(ServiceType serviceType, String title, String description) {
        checkCanEdit(1);
        this.serviceType = serviceType;
        this.title = title;
        this.description = description;
        completeStep(1);
    }

    public void updateAddress(Address address) {
        checkCanEdit(2);
        this.address = address;
        completeStep(2);
    }

    public void updateConfirmation(
            Priority priority, LocalDate preferredDate, BigDecimal estimatedValue, Boolean termsAccepted) {
        checkCanEdit(3);
        this.priority = priority;
        this.preferredDate = preferredDate;
        this.estimatedValue = estimatedValue;
        this.termsAccepted = termsAccepted;
        completeStep(3);
    }

    public void submit() {
        moveTo(SolicitationStatus.SUBMITTED);
        submittedAt = Instant.now();
    }

    public void startReview(UUID analystId) {
        moveTo(SolicitationStatus.IN_REVIEW);
        this.analystId = analystId;
    }

    public void decide(Decision decision, String comment, UUID analystId) {
        moveTo(decision == Decision.APPROVE ? SolicitationStatus.APPROVED : SolicitationStatus.REJECTED);
        this.analystId = analystId;
        this.decisionComment = comment;
        this.decidedAt = Instant.now();
    }

    // A step can be saved again, but only after the previous ones are done
    private void checkCanEdit(int step) {
        if (!isDraft()) {
            throw new ConflictException("Only a DRAFT can be edited");
        }
        if (step > currentStep + 1) {
            throw new ConflictException("Complete step " + (currentStep + 1) + " first");
        }
    }

    private void completeStep(int step) {
        currentStep = Math.max(currentStep, step);
    }

    private void moveTo(SolicitationStatus target) {
        if (!status.canMoveTo(target)) {
            throw new ConflictException("Cannot move from " + status + " to " + target);
        }
        status = target;
    }
}
