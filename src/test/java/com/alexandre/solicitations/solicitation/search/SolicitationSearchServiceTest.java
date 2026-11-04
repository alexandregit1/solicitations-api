package com.alexandre.solicitations.solicitation.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alexandre.solicitations.auth.AuthenticatedUser;
import com.alexandre.solicitations.shared.BrazilianState;
import com.alexandre.solicitations.user.Role;
import com.alexandre.solicitations.user.UserService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Query;

class SolicitationSearchServiceTest {

    private final ElasticsearchOperations operations = mock();
    private final UserService userService = mock();
    private final SolicitationSearchService service =
            new SolicitationSearchService(mock(SolicitationSearchRepository.class), operations, userService);

    private final AuthenticatedUser analyst = new AuthenticatedUser(UUID.randomUUID(), Role.ANALYST);

    @Test
    void analystAskingForAnotherUfGetsNothingAndElasticsearchIsNotQueried() {
        when(userService.getStates(analyst.id())).thenReturn(Set.of(BrazilianState.SP));

        SolicitationSearchResponse response = service.search(searchFor(BrazilianState.RJ), analyst);

        assertThat(response.items()).isEmpty();
        assertThat(response.total()).isZero();
        verify(operations, never()).search(any(Query.class), eq(SolicitationDocument.class));
    }

    private static SolicitationSearchRequest searchFor(BrazilianState state) {
        return new SolicitationSearchRequest(null, null, null, null, state, null, null, null, null, null);
    }
}
