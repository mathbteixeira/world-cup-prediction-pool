package io.github.mathbteixeira.worldcuppredictionpool.scoring;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.DefaultPredictionScoringEngine;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.MatchScoreInput;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.PredictionScoreInput;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoreBreakdown;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoringRuleDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPredictionScoringEngineTest {

    private final DefaultPredictionScoringEngine engine = new DefaultPredictionScoringEngine();
    private final ScoringRuleDefinition rule = ScoringRuleDefinition.defaultV1();

    @Test
    void shouldAwardExactScorePoints() {
        ScoreBreakdown breakdown = engine.score(new PredictionScoreInput(2, 1), new MatchScoreInput(2, 1), rule);

        assertThat(breakdown.totalPoints()).isEqualTo(7);
        assertThat(breakdown.exactScorePointsAwarded()).isEqualTo(7);
        assertThat(breakdown.outcomePointsAwarded()).isZero();
        assertThat(breakdown.goalDifferenceBonusPointsAwarded()).isZero();
    }

    @Test
    void shouldAwardOutcomePlusGoalDifferenceBonus() {
        ScoreBreakdown breakdown = engine.score(new PredictionScoreInput(3, 2), new MatchScoreInput(2, 1), rule);

        assertThat(breakdown.totalPoints()).isEqualTo(5);
        assertThat(breakdown.outcomePointsAwarded()).isEqualTo(3);
        assertThat(breakdown.goalDifferenceBonusPointsAwarded()).isEqualTo(2);
    }

    @Test
    void shouldAwardOutcomeOnly() {
        ScoreBreakdown breakdown = engine.score(new PredictionScoreInput(1, 0), new MatchScoreInput(3, 1), rule);

        assertThat(breakdown.totalPoints()).isEqualTo(3);
        assertThat(breakdown.outcomePointsAwarded()).isEqualTo(3);
        assertThat(breakdown.goalDifferenceBonusPointsAwarded()).isZero();
    }

    @Test
    void shouldAwardZeroForWrongPrediction() {
        ScoreBreakdown breakdown = engine.score(new PredictionScoreInput(0, 1), new MatchScoreInput(2, 1), rule);

        assertThat(breakdown.totalPoints()).isZero();
        assertThat(breakdown.explanation()).isEqualTo("Wrong prediction");
    }

    @Test
    void shouldAwardZeroForNoPrediction() {
        ScoreBreakdown breakdown = engine.score(null, new MatchScoreInput(2, 1), rule);

        assertThat(breakdown.totalPoints()).isZero();
        assertThat(breakdown.explanation()).isEqualTo("No prediction submitted");
    }
}
