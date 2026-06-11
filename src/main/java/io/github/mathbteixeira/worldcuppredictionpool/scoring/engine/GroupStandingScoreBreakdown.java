package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

public record GroupStandingScoreBreakdown(
        int totalPoints,
        int correctPositions,
        String explanation
) {
    public static GroupStandingScoreBreakdown noPrediction() {
        return new GroupStandingScoreBreakdown(0, 0, "No group prediction submitted");
    }
}