package com.alexandre.solicitations.dto;

import com.alexandre.solicitations.model.enums.ServiceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Step 1. Text is trimmed before validation, so "  ab  " fails the 3-character minimum. */
public record BasicInfoRequest(
        @NotNull ServiceType serviceType,
        @NotNull @Size(min = 3, max = 80) String title,
        @NotNull @Size(min = 20, max = 1000) String description) {

    public BasicInfoRequest {
        title = title == null ? null : title.strip();
        description = description == null ? null : description.strip();
    }
}
