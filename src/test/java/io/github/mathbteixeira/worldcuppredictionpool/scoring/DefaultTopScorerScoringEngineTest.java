package io.github.mathbteixeira.worldcuppredictionpool.scoring;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.DefaultTopScorerScoringEngine;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoringRuleDefinition;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultTopScorerScoringEngineTest {

    private final DefaultTopScorerScoringEngine engine = new DefaultTopScorerScoringEngine();
    private final ScoringRuleDefinition rule = ScoringRuleDefinition.defaultV1();

    @Test
    void shouldAwardPlayerAndGoalsPointsWhenBothMatch() {
        UUID player = UUID.randomUUID();

        var breakdown = engine.score(player, 7, player, 7, rule);

        assertThat(breakdown.totalPoints()).isEqualTo(30);
        assertThat(breakdown.playerPointsAwarded()).isEqualTo(20);
        assertThat(breakdown.goalsPointsAwarded()).isEqualTo(10);
    }

    @Test
    void shouldAwardOnlyPlayerPointsWhenGoalsDoNotMatch() {
        UUID player = UUID.randomUUID();

        var breakdown = engine.score(player, 6, player, 7, rule);

        assertThat(breakdown.totalPoints()).isEqualTo(20);
        assertThat(breakdown.playerPointsAwarded()).isEqualTo(20);
        assertThat(breakdown.goalsPointsAwarded()).isZero();
    }

    @Test
    void shouldNotAwardGoalsPointsWhenPlayerDoesNotMatch() {
        var breakdown = engine.score(UUID.randomUUID(), 7, UUID.randomUUID(), 7, rule);

        assertThat(breakdown.totalPoints()).isZero();
        assertThat(breakdown.playerPointsAwarded()).isZero();
        assertThat(breakdown.goalsPointsAwarded()).isZero();
    }
}
