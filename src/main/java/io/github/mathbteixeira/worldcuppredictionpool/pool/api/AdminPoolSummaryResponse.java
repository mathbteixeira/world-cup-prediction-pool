package io.github.mathbteixeira.worldcuppredictionpool.pool.api;

import java.util.UUID;

public record AdminPoolSummaryResponse(
        UUID id,
        UUID tournamentId,
        UUID singleMatchId,
        String poolScope,
        String name,
        String description,
        String inviteCode,
        PoolMemberResponse owner,
        long memberCount
) {
}
