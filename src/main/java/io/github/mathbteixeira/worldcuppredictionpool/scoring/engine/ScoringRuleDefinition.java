package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

public record ScoringRuleDefinition(
        int version,
        int exactScorePoints,
        int outcomePoints,
        int goalDifferenceBonusPoints
) {
    public static ScoringRuleDefinition defaultV1() {
        return new ScoringRuleDefinition(1, 5, 3, 2);
    }
}
