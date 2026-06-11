package io.github.mathbteixeira.worldcuppredictionpool.scoring;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.DefaultTournamentRankingScoringEngine;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoringRuleDefinition;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.TournamentRankingScoreBreakdown;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultTournamentRankingScoringEngineTest {

    private final DefaultTournamentRankingScoringEngine engine = new DefaultTournamentRankingScoringEngine();
    private final ScoringRuleDefinition rule = ScoringRuleDefinition.defaultV1();

    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final UUID third = UUID.randomUUID();
    private final UUID fourth = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @Test
    void shouldAwardFullPodiumPoints() {
        TournamentRankingScoreBreakdown breakdown =
                engine.score(List.of(first, second, third, fourth), List.of(first, second, third, fourth), rule);

        assertThat(breakdown.championPointsAwarded()).isEqualTo(20);
        assertThat(breakdown.runnerUpPointsAwarded()).isEqualTo(18);
        assertThat(breakdown.thirdPlacePointsAwarded()).isEqualTo(15);
        assertThat(breakdown.fourthPlacePointsAwarded()).isEqualTo(15);
        assertThat(breakdown.totalPoints()).isEqualTo(68);
    }

    @Test
    void shouldAwardChampionPointsOnly() {
        TournamentRankingScoreBreakdown breakdown =
                engine.score(List.of(first, other, other, other), List.of(first, second, third, fourth), rule);

        assertThat(breakdown.championPointsAwarded()).isEqualTo(20);
        assertThat(breakdown.runnerUpPointsAwarded()).isZero();
        assertThat(breakdown.totalPoints()).isEqualTo(20);
    }

    @Test
    void shouldScoreEachPositionIndependently() {
        // third and fourth correct only
        TournamentRankingScoreBreakdown breakdown =
                engine.score(List.of(second, first, third, fourth), List.of(first, second, third, fourth), rule);

        assertThat(breakdown.championPointsAwarded()).isZero();
        assertThat(breakdown.runnerUpPointsAwarded()).isZero();
        assertThat(breakdown.thirdPlacePointsAwarded()).isEqualTo(15);
        assertThat(breakdown.fourthPlacePointsAwarded()).isEqualTo(15);
        assertThat(breakdown.totalPoints()).isEqualTo(30);
    }

    @Test
    void shouldAwardZeroForNoPrediction() {
        TournamentRankingScoreBreakdown breakdown =
                engine.score(null, List.of(first, second, third, fourth), rule);

        assertThat(breakdown.totalPoints()).isZero();
        assertThat(breakdown.explanation()).isEqualTo("No final-ranking prediction submitted");
    }
}