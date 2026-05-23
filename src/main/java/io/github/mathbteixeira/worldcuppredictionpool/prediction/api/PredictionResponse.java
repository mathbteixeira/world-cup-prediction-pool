package io.github.mathbteixeira.worldcuppredictionpool.prediction.api;

import java.time.Instant;
import java.util.UUID;

public record PredictionResponse(
        UUID id,
        UUID poolId,
        UUID matchId,
        UUID userId,
        int homeScore,
        int awayScore,
        Instant submittedAt
) {
}
