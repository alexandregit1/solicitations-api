package com.alexandre.solicitations.dto;

import com.alexandre.solicitations.model.enums.BrazilianState;
import com.alexandre.solicitations.model.enums.Priority;
import com.alexandre.solicitations.model.enums.ServiceType;
import com.alexandre.solicitations.model.enums.SolicitationStatus;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Query parameters of GET /analyst/solicitations/search. Every filter is optional. */
public record SolicitationSearchRequest(
        @Parameter(description = "Free text over title, description, neighborhood and city")
        String q,

        SolicitationStatus status,
        ServiceType serviceType,
        Priority priority,

        @Parameter(description = "UF. For an ANALYST it can only narrow down their coverage")
        BrazilianState state,

        @Parameter(description = "Created on or after (yyyy-MM-dd)") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateFrom,

        @Parameter(description = "Created on or before (yyyy-MM-dd)") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateTo,
        // Elasticsearch pages through the first 10,000 hits only: 100 pages of up to 100
        @Min(0) @Max(99) Integer page,
        @Min(1) @Max(100) Integer size,

        @Parameter(
                description = "field,direction with field createdAt, preferredDate or estimatedValue."
                        + " Default: relevance when q is given, else createdAt,desc")
        @Pattern(
                regexp = "(createdAt|preferredDate|estimatedValue),(asc|desc)",
                message = "must be field,direction, e.g. createdAt,desc")
        String sort) {

    public SolicitationSearchRequest {
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
