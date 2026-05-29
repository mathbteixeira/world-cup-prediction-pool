package io.github.mathbteixeira.worldcuppredictionpool.prediction.api;

import java.util.UUID;

public record PredictionUserResponse(
        UUID userId,
        String username
) {
}
