package io.github.mathbteixeira.worldcuppredictionpool.topscorer.api;

import java.util.UUID;

public record TopScorerRecalculationResponse(
        UUID tournamentId,
        TopScorerPick topScorer,
        String resultChecksum,
        int scoredPredictions,
        int affectedPools,
        boolean idempotentReplay
) {
}
