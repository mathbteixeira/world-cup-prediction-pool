package io.github.mathbteixeira.worldcuppredictionpool.topscorer.api;

import jakarta.validation.constraints.NotNull;

public record TopScorerValidationRequest(
        @NotNull Boolean playerCorrect,
        @NotNull Boolean goalsCorrect
) {
}
