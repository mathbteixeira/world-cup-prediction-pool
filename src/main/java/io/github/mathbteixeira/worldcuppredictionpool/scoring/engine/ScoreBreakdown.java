package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

public record ScoreBreakdown(
        int totalPoints,
        int exactScorePointsAwarded,
        int outcomePointsAwarded,
        int goalDifferenceBonusPointsAwarded,
        String explanation
) {
    public static ScoreBreakdown noPrediction() {
        return new ScoreBreakdown(0, 0, 0, 0, "No prediction submitted");
    }
}
