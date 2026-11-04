package com.alexandre.solicitations.solicitation.search;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SolicitationSearchResponse(List<Item> items, int page, int size, long total) {

    public record Item(
            String id,
            String status,
            String serviceType,
            String title,
            String city,
            String state,
            String priority,
            LocalDate preferredDate,
            Double estimatedValue,
            Instant createdAt) {

        static Item from(SolicitationDocument doc) {
            return new Item(
                    doc.getId(),
                    doc.getStatus(),
                    doc.getServiceType(),
                    doc.getTitle(),
                    doc.getCity(),
                    doc.getState(),
                    doc.getPriority(),
                    doc.getPreferredDate(),
                    doc.getEstimatedValue(),
                    doc.getCreatedAt());
        }
    }
}
