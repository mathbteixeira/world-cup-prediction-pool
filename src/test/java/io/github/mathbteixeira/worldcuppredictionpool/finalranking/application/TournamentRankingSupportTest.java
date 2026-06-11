package io.github.mathbteixeira.worldcuppredictionpool.finalranking.application;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TournamentRankingSupportTest {

    @Mock
    private TeamRepository teamRepository;

    @Test
    void finalRankingPredictionsCloseAtEndOfJuneFifteenthInBrazil() {
        TournamentRankingSupport support = new TournamentRankingSupport(teamRepository);

        assertThat(support.predictionDeadline(UUID.randomUUID()))
                .isEqualTo(Instant.parse("2026-06-16T03:00:00Z"));
    }
}
