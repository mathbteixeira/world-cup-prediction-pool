package io.github.mathbteixeira.worldcuppredictionpool.scoring;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.DefaultGroupStandingScoringEngine;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.GroupStandingScoreBreakdown;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoringRuleDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultGroupStandingScoringEngineTest {

    private final DefaultGroupStandingScoringEngine engine = new DefaultGroupStandingScoringEngine();
    private final ScoringRuleDefinition rule = ScoringRuleDefinition.defaultV1();

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID c = UUID.randomUUID();
    private final UUID d = UUID.randomUUID();

    @Test
    void shouldAward10PerCorrectPosition() {
        GroupStandingScoreBreakdown breakdown = engine.score(List.of(a, b, c, d), List.of(a, b, c, d), rule);

        assertThat(breakdown.correctPositions()).isEqualTo(4);
        assertThat(breakdown.totalPoints()).isEqualTo(40);
    }

    @Test
    void shouldAwardOnlyForCorrectlyPlacedTeams() {
        // first two swapped -> only positions 3 and 4 correct
        GroupStandingScoreBreakdown breakdown = engine.score(List.of(b, a, c, d), List.of(a, b, c, d), rule);

        assertThat(breakdown.correctPositions()).isEqualTo(2);
        assertThat(breakdown.totalPoints()).isEqualTo(20);
    }

    @Test
    void shouldAwardZeroWhenNoPositionsMatch() {
        GroupStandingScoreBreakdown breakdown = engine.score(List.of(b, c, d, a), List.of(a, b, c, d), rule);

        assertThat(breakdown.correctPositions()).isZero();
        assertThat(breakdown.totalPoints()).isZero();
        assertThat(breakdown.explanation()).isEqualTo("No correct group positions");
    }

    @Test
    void shouldAwardZeroForNoPrediction() {
        GroupStandingScoreBreakdown breakdown = engine.score(null, List.of(a, b, c, d), rule);

        assertThat(breakdown.totalPoints()).isZero();
        assertThat(breakdown.explanation()).isEqualTo("No group prediction submitted");
    }

    @Test
    void shouldHonourConfiguredPerPositionPoints() {
        ScoringRuleDefinition custom = new ScoringRuleDefinition(2, 7, 3, 2, 5, 20, 18, 15, 15);

        GroupStandingScoreBreakdown breakdown = engine.score(List.of(a, b, c, d), List.of(a, b, c, d), custom);

        assertThat(breakdown.totalPoints()).isEqualTo(20); // 4 * 5
    }
}