package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

import java.util.List;
import java.util.UUID;

public interface TournamentRankingScoringEngine {

    /**
     * Scores a predicted podium against the official final ranking.
     *
     * @param predictedOrder predicted team ids ordered champion..4th (nullable)
     * @param actualOrder    official team ids ordered champion..4th
     * @param rule           active scoring rule supplying the per-position awards
     */
    TournamentRankingScoreBreakdown score(List<UUID> predictedOrder, List<UUID> actualOrder, ScoringRuleDefinition rule);
}