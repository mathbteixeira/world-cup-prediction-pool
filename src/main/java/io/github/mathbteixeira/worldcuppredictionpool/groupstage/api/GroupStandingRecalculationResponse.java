package io.github.mathbteixeira.worldcuppredictionpool.groupstage.api;

import java.util.List;
import java.util.UUID;

/**
 * Admin response after confirming a group's official standings and recalculating
 * the affected predictions and leaderboards.
 */
public record GroupStandingRecalculationResponse(
        UUID tournamentId,
        String groupName,
        List<UUID> officialTeamIdsByPosition,
        String resultChecksum,
        int scoredPredictions,
        int affectedPools,
        boolean idempotentReplay
) {
}