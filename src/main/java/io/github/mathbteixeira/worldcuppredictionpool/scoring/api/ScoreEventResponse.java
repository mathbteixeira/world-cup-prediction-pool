package io.github.mathbteixeira.worldcuppredictionpool.scoring.api;

import java.time.Instant;
import java.util.UUID;

public record ScoreEventResponse(
        UUID id,
        UUID matchId,
        UUID predictionId,
        UUID userId,
        int pointsAwarded,
        int ruleVersion,
        String resultChecksum,
        Instant createdAt
) {
}
