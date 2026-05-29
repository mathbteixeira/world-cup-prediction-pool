package io.github.mathbteixeira.worldcuppredictionpool.tournament.api;

public record MatchResultResponse(
        int homeScore,
        int awayScore,
        Integer homePenaltyScore,
        Integer awayPenaltyScore,
        boolean finalResult
) {
}
