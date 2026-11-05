package com.alexandre.solicitations.dto;

import com.alexandre.solicitations.model.enums.Priority;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Step 3. */
public record ConfirmationRequest(
        @NotNull Priority priority,
        @NotNull @FutureOrPresent LocalDate preferredDate,

        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2)
        BigDecimal estimatedValue,

        @NotNull @AssertTrue(message = "must be accepted") Boolean termsAccepted) {

    private static final BigDecimal HIGH_PRIORITY_MIN_VALUE = BigDecimal.valueOf(100);

    @JsonIgnore
    @AssertTrue(message = "estimatedValue must be at least 100 when priority is HIGH")
    public boolean isEstimatedValueValidForPriority() {
        return priority != Priority.HIGH
                || estimatedValue == null
                || estimatedValue.compareTo(HIGH_PRIORITY_MIN_VALUE) >= 0;
    }
}
