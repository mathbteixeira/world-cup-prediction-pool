package io.github.mathbteixeira.worldcuppredictionpool.pool.api;

import java.time.Instant;
import java.util.UUID;

public record PoolLeaderboardEntryResponse(
        UUID poolId,
        UUID userId,
        UUID managedParticipantId,
        String username,
        int totalPoints,
        int rankPosition,
        Instant recalculatedAt
) {
}
