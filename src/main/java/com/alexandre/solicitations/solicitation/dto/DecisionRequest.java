package com.alexandre.solicitations.solicitation.dto;

import com.alexandre.solicitations.solicitation.Decision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionRequest(
        @NotNull Decision decision,
        @NotNull @Size(min = 10, max = 1000) String comment) {

    public DecisionRequest {
        comment = comment == null ? null : comment.strip();
    }
}
