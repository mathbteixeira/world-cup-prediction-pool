package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

public interface TopScorerScoringEngine {
    TopScorerScoreBreakdown score(boolean playerCorrect,
                                  boolean goalsCorrect,
                                  ScoringRuleDefinition rule);
}
