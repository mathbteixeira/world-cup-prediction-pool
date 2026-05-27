package io.github.mathbteixeira.worldcuppredictionpool.common.api;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        String httpStatus,
        int statusCode,
        String message,
        String path
) {
}
