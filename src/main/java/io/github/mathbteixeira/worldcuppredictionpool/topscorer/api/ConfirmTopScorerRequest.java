package io.github.mathbteixeira.worldcuppredictionpool.topscorer.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmTopScorerRequest(
        @NotNull UUID playerId,
        @Min(1) @Max(15) int goals
) {
}
