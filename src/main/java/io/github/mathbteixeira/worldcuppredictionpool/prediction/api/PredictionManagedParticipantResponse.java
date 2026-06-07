package io.github.mathbteixeira.worldcuppredictionpool.prediction.api;

import java.util.UUID;

public record PredictionManagedParticipantResponse(
        UUID participantId,
        String name
) {
}
