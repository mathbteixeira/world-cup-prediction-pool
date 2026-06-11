package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

public record TopScorerScoreBreakdown(
        int totalPoints,
        int playerPointsAwarded,
        int goalsPointsAwarded,
        String explanation
) {
}
