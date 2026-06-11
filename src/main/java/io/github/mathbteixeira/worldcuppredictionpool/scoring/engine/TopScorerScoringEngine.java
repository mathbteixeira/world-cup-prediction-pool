package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

import java.util.UUID;

public interface TopScorerScoringEngine {
    TopScorerScoreBreakdown score(UUID predictedPlayerId,
                                  int predictedGoals,
                                  UUID actualPlayerId,
                                  int actualGoals,
                                  ScoringRuleDefinition rule);
}
