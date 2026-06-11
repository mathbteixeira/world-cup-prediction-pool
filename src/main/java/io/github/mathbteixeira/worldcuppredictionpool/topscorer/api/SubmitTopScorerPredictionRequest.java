package io.github.mathbteixeira.worldcuppredictionpool.topscorer.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SubmitTopScorerPredictionRequest(
        @NotNull UUID teamId,
        @NotBlank @Size(max = 120) String playerName,
        @Min(1) @Max(15) int goals
) {
}
