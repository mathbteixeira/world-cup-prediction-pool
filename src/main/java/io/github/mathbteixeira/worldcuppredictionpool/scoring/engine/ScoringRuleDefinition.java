package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

public record ScoringRuleDefinition(
        int version,
        int exactScorePoints,
        int outcomePoints,
        int goalDifferenceBonusPoints,
        int groupPositionPoints,
        int championPoints,
        int runnerUpPoints,
        int thirdPlacePoints,
        int fourthPlacePoints
) {
    public static ScoringRuleDefinition defaultV1() {
        return new ScoringRuleDefinition(1, 7, 3, 2, 10, 20, 18, 15, 15);
    }
}