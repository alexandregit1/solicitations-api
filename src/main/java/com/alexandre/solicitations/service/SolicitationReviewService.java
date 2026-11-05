package com.alexandre.solicitations.service;

import com.alexandre.solicitations.aspect.Audit;
import com.alexandre.solicitations.dto.DecisionRequest;
import com.alexandre.solicitations.dto.SolicitationResponse;
import com.alexandre.solicitations.exception.ForbiddenException;
import com.alexandre.solicitations.exception.NotFoundException;
import com.alexandre.solicitations.model.Solicitation;
import com.alexandre.solicitations.model.enums.Role;
import com.alexandre.solicitations.repository.SolicitationRepository;
import com.alexandre.solicitations.security.AuthenticatedUser;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The ANALYST side. An ANALYST only reaches solicitations in the UFs they cover; an ADMIN reaches all. */
@Service
@RequiredArgsConstructor
public class SolicitationReviewService {

    private final SolicitationRepository repository;
    private final UserService userService;
    private final SolicitationSearchService searchService;

    @Transactional(readOnly = true)
    public SolicitationResponse get(UUID id, AuthenticatedUser reviewer) {
        return SolicitationResponse.from(findReviewable(id, reviewer));
    }

    @Audit("REVIEW_STARTED")
    @Transactional
    public SolicitationResponse start(UUID id, AuthenticatedUser reviewer) {
        Solicitation solicitation = findReviewable(id, reviewer);
        solicitation.startReview(reviewer.id());
        return save(solicitation);
    }

    @Audit("SOLICITATION_DECIDED")
    @Transactional
    public SolicitationResponse decide(UUID id, AuthenticatedUser reviewer, DecisionRequest request) {
        Solicitation solicitation = findReviewable(id, reviewer);
        solicitation.decide(request.decision(), request.comment(), reviewer.id());
        return save(solicitation);
    }

    // Drafts are private to the client: for a reviewer they don't exist yet
    private Solicitation findReviewable(UUID id, AuthenticatedUser reviewer) {
        Solicitation solicitation = repository
                .findById(id)
                .filter(s -> !s.isDraft())
                .orElseThrow(() -> new NotFoundException("Solicitation not found"));
        if (reviewer.is(Role.ANALYST)
                && !userService
                        .getStates(reviewer.id())
                        .contains(solicitation.getAddress().getState())) {
            throw new ForbiddenException("This solicitation is outside your UF coverage");
        }
        return solicitation;
    }

    private SolicitationResponse save(Solicitation solicitation) {
        Solicitation saved = repository.save(solicitation);
        searchService.index(saved);
        return SolicitationResponse.from(saved);
    }
}
