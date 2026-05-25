package io.github.mathbteixeira.worldcuppredictionpool.prediction.api;

import java.time.Instant;
import java.util.UUID;

public record PredictionResponse(
        UUID predictionId,
        UUID poolId,
        UUID matchId,
        int homeScore,
        int awayScore,
        Instant submittedAt
) {
}
