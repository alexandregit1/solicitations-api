package com.alexandre.solicitations.solicitation.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.alexandre.solicitations.auth.AuthenticatedUser;
import com.alexandre.solicitations.shared.BrazilianState;
import com.alexandre.solicitations.solicitation.Solicitation;
import com.alexandre.solicitations.solicitation.SolicitationStatus;
import com.alexandre.solicitations.user.Role;
import com.alexandre.solicitations.user.UserService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

/** PostgreSQL is the source of truth; Elasticsearch keeps a copy for searching. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitationSearchService {

    // Title matches weigh more
    private static final List<String> TEXT_FIELDS = List.of("title^3", "description", "neighborhood", "city");

    private final SolicitationSearchRepository repository;
    private final ElasticsearchOperations operations;
    private final UserService userService;

    // An indexing failure is logged and doesn't fail the request: the data is safe in PostgreSQL
    public void index(Solicitation solicitation) {
        try {
            repository.save(SolicitationDocument.from(solicitation));
        } catch (RuntimeException ex) {
            log.error("Could not index solicitation {}", solicitation.getId(), ex);
        }
    }

    public SolicitationSearchResponse search(SolicitationSearchRequest request, AuthenticatedUser user) {
        List<BrazilianState> states = allowedStates(request.state(), user);
        if (user.is(Role.ANALYST) && states.isEmpty()) {
            return new SolicitationSearchResponse(List.of(), request.page(), request.size(), 0);
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(filters(request, states)))
                .withPageable(PageRequest.of(request.page(), request.size(), sort(request)))
                .withTrackTotalHits(true)
                .build();
        SearchHits<SolicitationDocument> hits = operations.search(query, SolicitationDocument.class);

        List<SolicitationSearchResponse.Item> items = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(SolicitationSearchResponse.Item::from)
                .toList();
        return new SolicitationSearchResponse(items, request.page(), request.size(), hits.getTotalHits());
    }

    /**
     * UFs to filter by (empty = all). For an ANALYST the coverage is always applied, whatever the request
     * says: asking for a UF outside it returns nothing, never more data.
     */
    private List<BrazilianState> allowedStates(BrazilianState requested, AuthenticatedUser user) {
        if (!user.is(Role.ANALYST)) {
            return requested == null ? List.of() : List.of(requested);
        }
        Set<BrazilianState> coverage = userService.getStates(user.id());
        if (requested == null) {
            return List.copyOf(coverage);
        }
        return coverage.contains(requested) ? List.of(requested) : List.of();
    }

    private static BoolQuery filters(SolicitationSearchRequest request, List<BrazilianState> states) {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        if (hasText(request.q())) {
            bool.must(m -> m.multiMatch(
                    mm -> mm.query(request.q().strip()).fields(TEXT_FIELDS).fuzziness("AUTO")));
        }

        // Drafts are private to the client
        bool.mustNot(m -> m.term(t -> t.field("status").value(SolicitationStatus.DRAFT.name())));
        term(bool, "status", request.status());
        term(bool, "serviceType", request.serviceType());
        term(bool, "priority", request.priority());
        if (!states.isEmpty()) {
            List<FieldValue> values =
                    states.stream().map(state -> FieldValue.of(state.name())).toList();
            bool.filter(f -> f.terms(t -> t.field("state").terms(v -> v.value(values))));
        }

        if (request.dateFrom() != null || request.dateTo() != null) {
            bool.filter(f -> f.range(r -> r.date(d -> {
                d.field("createdAt");
                if (request.dateFrom() != null) {
                    d.gte(startOfDay(request.dateFrom()));
                }
                if (request.dateTo() != null) {
                    d.lt(startOfDay(request.dateTo().plusDays(1)));
                }
                return d;
            })));
        }
        return bool.build();
    }

    private static void term(BoolQuery.Builder bool, String field, Enum<?> value) {
        if (value != null) {
            bool.filter(f -> f.term(t -> t.field(field).value(value.name())));
        }
    }

    // Calendar days in the server zone (TZ, America/Sao_Paulo in compose.yaml)
    private static String startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toString();
    }

    // Unsorted keeps Elasticsearch's relevance order
    private static Sort sort(SolicitationSearchRequest request) {
        if (request.sort() == null) {
            return hasText(request.q()) ? Sort.unsorted() : Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = request.sort().split(",");
        return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
