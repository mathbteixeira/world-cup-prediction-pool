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
        int fourthPlacePoints,
        int topScorerPlayerPoints,
        int topScorerGoalsPoints
) {
    public ScoringRuleDefinition(int version,
                                 int exactScorePoints,
                                 int outcomePoints,
                                 int goalDifferenceBonusPoints,
                                 int groupPositionPoints,
                                 int championPoints,
                                 int runnerUpPoints,
                                 int thirdPlacePoints,
                                 int fourthPlacePoints) {
        this(version, exactScorePoints, outcomePoints, goalDifferenceBonusPoints, groupPositionPoints,
                championPoints, runnerUpPoints, thirdPlacePoints, fourthPlacePoints, 20, 10);
    }

    public static ScoringRuleDefinition defaultV1() {
        return new ScoringRuleDefinition(1, 7, 3, 2, 10, 20, 18, 15, 15, 20, 10);
    }
}
