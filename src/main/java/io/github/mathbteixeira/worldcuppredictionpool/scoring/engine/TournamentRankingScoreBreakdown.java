package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

public record TournamentRankingScoreBreakdown(
        int totalPoints,
        int championPointsAwarded,
        int runnerUpPointsAwarded,
        int thirdPlacePointsAwarded,
        int fourthPlacePointsAwarded,
        String explanation
) {
    public static TournamentRankingScoreBreakdown noPrediction() {
        return new TournamentRankingScoreBreakdown(0, 0, 0, 0, 0, "No final-ranking prediction submitted");
    }
}