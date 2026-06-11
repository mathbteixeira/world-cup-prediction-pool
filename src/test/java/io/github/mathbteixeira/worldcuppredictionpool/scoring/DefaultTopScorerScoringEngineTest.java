package io.github.mathbteixeira.worldcuppredictionpool.scoring;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.DefaultTopScorerScoringEngine;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoringRuleDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultTopScorerScoringEngineTest {

    private final DefaultTopScorerScoringEngine engine = new DefaultTopScorerScoringEngine();
    private final ScoringRuleDefinition rule = ScoringRuleDefinition.defaultV1();

    @Test
    void shouldAwardPlayerAndGoalsPointsWhenBothMatch() {
        var breakdown = engine.score(true, true, rule);

        assertThat(breakdown.totalPoints()).isEqualTo(30);
        assertThat(breakdown.playerPointsAwarded()).isEqualTo(20);
        assertThat(breakdown.goalsPointsAwarded()).isEqualTo(10);
    }

    @Test
    void shouldAwardOnlyPlayerPointsWhenGoalsDoNotMatch() {
        var breakdown = engine.score(true, false, rule);

        assertThat(breakdown.totalPoints()).isEqualTo(20);
        assertThat(breakdown.playerPointsAwarded()).isEqualTo(20);
        assertThat(breakdown.goalsPointsAwarded()).isZero();
    }

    @Test
    void shouldNotAwardGoalsPointsWhenPlayerDoesNotMatch() {
        var breakdown = engine.score(false, true, rule);

        assertThat(breakdown.totalPoints()).isZero();
        assertThat(breakdown.playerPointsAwarded()).isZero();
        assertThat(breakdown.goalsPointsAwarded()).isZero();
    }
}
