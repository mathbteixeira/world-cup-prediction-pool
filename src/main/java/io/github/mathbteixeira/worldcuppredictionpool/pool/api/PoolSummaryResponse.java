package io.github.mathbteixeira.worldcuppredictionpool.pool.api;

import java.util.UUID;

public record PoolSummaryResponse(
        UUID id,
        UUID tournamentId,
        UUID singleMatchId,
        String poolScope,
        String name,
        String description,
        String inviteCode,
        String membershipRole
) {
}
