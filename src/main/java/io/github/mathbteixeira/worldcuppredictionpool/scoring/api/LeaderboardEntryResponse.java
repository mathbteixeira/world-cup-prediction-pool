package io.github.mathbteixeira.worldcuppredictionpool.scoring.api;

import java.util.UUID;

public record LeaderboardEntryResponse(
        UUID userId,
        String username,
        int totalPoints,
        int rankPosition
) {
}
