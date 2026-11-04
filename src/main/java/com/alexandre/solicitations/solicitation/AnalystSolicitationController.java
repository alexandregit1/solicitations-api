package com.alexandre.solicitations.solicitation;

import com.alexandre.solicitations.auth.AuthenticatedUser;
import com.alexandre.solicitations.solicitation.dto.DecisionRequest;
import com.alexandre.solicitations.solicitation.dto.SolicitationResponse;
import com.alexandre.solicitations.solicitation.search.SolicitationSearchRequest;
import com.alexandre.solicitations.solicitation.search.SolicitationSearchResponse;
import com.alexandre.solicitations.solicitation.search.SolicitationSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analyst")
@RestController
@RequestMapping("/analyst/solicitations")
@RequiredArgsConstructor
public class AnalystSolicitationController {

    private final SolicitationReviewService reviewService;
    private final SolicitationSearchService searchService;

    @Operation(summary = "Searches with filters and pagination. An ANALYST only gets their UFs")
    @GetMapping("/search")
    public SolicitationSearchResponse search(
            @Valid @ParameterObject SolicitationSearchRequest request,
            @AuthenticationPrincipal AuthenticatedUser reviewer) {
        return searchService.search(request, reviewer);
    }

    @Operation(summary = "Returns a solicitation in the caller's UF coverage")
    @GetMapping("/{id}")
    public SolicitationResponse get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser reviewer) {
        return reviewService.get(id, reviewer);
    }

    @Operation(summary = "SUBMITTED → IN_REVIEW")
    @PostMapping("/{id}/start")
    public SolicitationResponse start(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser reviewer) {
        return reviewService.start(id, reviewer);
    }

    @Operation(summary = "APPROVE or REJECT, with a comment")
    @PostMapping("/{id}/decide")
    public SolicitationResponse decide(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser reviewer,
            @Valid @RequestBody DecisionRequest request) {
        return reviewService.decide(id, reviewer, request);
    }
}
