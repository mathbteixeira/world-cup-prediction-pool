package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

import java.util.List;
import java.util.UUID;

public interface GroupStandingScoringEngine {

    /**
     * Scores a predicted group ordering against the official ordering.
     *
     * @param predictedOrder predicted team ids ordered 1st..4th (nullable)
     * @param actualOrder    official team ids ordered 1st..4th
     * @param rule           active scoring rule supplying the per-position award
     */
    GroupStandingScoreBreakdown score(List<UUID> predictedOrder, List<UUID> actualOrder, ScoringRuleDefinition rule);
}