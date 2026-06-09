package io.github.mathbteixeira.worldcuppredictionpool.pool.api;

import java.util.UUID;

public record PoolMemberResponse(
        UUID userId,
        String username,
        String email,
        String role
) {
}
