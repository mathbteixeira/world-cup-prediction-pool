package io.github.mathbteixeira.worldcuppredictionpool.scoring.api;

import java.util.UUID;

public record MatchResultRecalculationResponse(
        UUID matchId,
        int homeScore,
        int awayScore,
        Integer homePenaltyScore,
        Integer awayPenaltyScore,
        boolean finalResult,
        String resultChecksum,
        int scoredPredictions,
        int affectedPools,
        boolean idempotentReplay
) {
}
