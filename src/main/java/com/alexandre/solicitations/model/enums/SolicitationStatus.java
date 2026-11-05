package com.alexandre.solicitations.model.enums;

import java.util.Set;

/** DRAFT → SUBMITTED → IN_REVIEW → APPROVED / REJECTED. IN_REVIEW is optional: a SUBMITTED one can be decided. */
public enum SolicitationStatus {
    DRAFT,
    SUBMITTED,
    IN_REVIEW,
    APPROVED,
    REJECTED;

    public boolean canMoveTo(SolicitationStatus target) {
        Set<SolicitationStatus> next = switch (this) {
            case DRAFT -> Set.of(SUBMITTED);
            case SUBMITTED -> Set.of(IN_REVIEW, APPROVED, REJECTED);
            case IN_REVIEW -> Set.of(APPROVED, REJECTED);
            case APPROVED, REJECTED -> Set.of();
        };
        return next.contains(target);
    }
}
