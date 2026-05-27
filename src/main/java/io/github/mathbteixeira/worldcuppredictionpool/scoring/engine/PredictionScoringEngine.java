package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

public interface PredictionScoringEngine {

    ScoreBreakdown score(PredictionScoreInput prediction, MatchScoreInput actualResult, ScoringRuleDefinition rule);
}
