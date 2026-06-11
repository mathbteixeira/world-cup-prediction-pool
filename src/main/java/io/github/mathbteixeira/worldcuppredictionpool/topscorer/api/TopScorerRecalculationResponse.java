package io.github.mathbteixeira.worldcuppredictionpool.topscorer.api;

import java.util.UUID;

public record TopScorerRecalculationResponse(
        UUID tournamentId,
        String resultChecksum,
        int scoredPredictions,
        int affectedPools,
        boolean idempotentReplay
) {
}
