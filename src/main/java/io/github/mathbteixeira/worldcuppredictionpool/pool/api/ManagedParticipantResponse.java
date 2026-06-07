package io.github.mathbteixeira.worldcuppredictionpool.pool.api;

import java.util.UUID;

public record ManagedParticipantResponse(
        UUID participantId,
        UUID poolId,
        String name
) {
}
