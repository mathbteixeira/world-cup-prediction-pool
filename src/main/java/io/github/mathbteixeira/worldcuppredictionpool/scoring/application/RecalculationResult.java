package io.github.mathbteixeira.worldcuppredictionpool.scoring.application;

import java.util.UUID;

public record RecalculationResult(
        UUID matchId,
        String resultChecksum,
        int scoredPredictions,
        int affectedPools,
        boolean idempotentReplay
) {
}
