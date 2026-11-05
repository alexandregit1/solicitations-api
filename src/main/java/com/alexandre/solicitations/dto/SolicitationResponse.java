package com.alexandre.solicitations.dto;

import com.alexandre.solicitations.model.Solicitation;
import com.alexandre.solicitations.model.enums.Priority;
import com.alexandre.solicitations.model.enums.ServiceType;
import com.alexandre.solicitations.model.enums.SolicitationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SolicitationResponse(
        UUID id,
        UUID clientId,
        SolicitationStatus status,
        int currentStep,
        ServiceType serviceType,
        String title,
        String description,
        AddressResponse address,
        Priority priority,
        LocalDate preferredDate,
        BigDecimal estimatedValue,
        Boolean termsAccepted,
        UUID analystId,
        String decisionComment,
        Instant createdAt,
        Instant submittedAt,
        Instant decidedAt) {

    public static SolicitationResponse from(Solicitation s) {
        return new SolicitationResponse(
                s.getId(),
                s.getClientId(),
                s.getStatus(),
                s.getCurrentStep(),
                s.getServiceType(),
                s.getTitle(),
                s.getDescription(),
                AddressResponse.from(s.getAddress()),
                s.getPriority(),
                s.getPreferredDate(),
                s.getEstimatedValue(),
                s.getTermsAccepted(),
                s.getAnalystId(),
                s.getDecisionComment(),
                s.getCreatedAt(),
                s.getSubmittedAt(),
                s.getDecidedAt());
    }
}
