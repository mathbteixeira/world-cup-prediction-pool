package io.github.mathbteixeira.worldcuppredictionpool.prediction.api;

import jakarta.validation.constraints.Min;

public record SubmitPredictionRequest(
        @Min(0) int homeScore,
        @Min(0) int awayScore
) {
}
