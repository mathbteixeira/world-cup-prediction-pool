package io.github.mathbteixeira.worldcuppredictionpool.prediction.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitPredictionRequest(
        @NotNull @Min(0) Integer homeScore,
        @NotNull @Min(0) Integer awayScore
) {
}
