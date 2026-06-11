package io.github.mathbteixeira.worldcuppredictionpool.topscorer.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TeamRepository;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TopScorerSupportTest {

    @Mock
    private PredictionPoolRepository predictionPoolRepository;

    @Mock
    private PoolMembershipRepository poolMembershipRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private TeamRepository teamRepository;

    @Test
    void topScorerPredictionsCloseAtEndOfJuneFifteenthInBrazil() {
        TopScorerSupport support = supportAt("2026-06-16T02:59:59Z");

        assertThat(support.predictionDeadline()).isEqualTo(Instant.parse("2026-06-16T03:00:00Z"));
        assertThat(support.predictionOpen()).isTrue();
        assertThat(supportAt("2026-06-16T03:00:00Z").predictionOpen()).isFalse();
    }

    private TopScorerSupport supportAt(String instant) {
        return new TopScorerSupport(
                predictionPoolRepository,
                poolMembershipRepository,
                userAccountRepository,
                teamRepository,
                Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
        );
    }
}
