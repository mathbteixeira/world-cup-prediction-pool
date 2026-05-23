package io.github.mathbteixeira.worldcuppredictionpool.pool.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePoolRequest(
        @NotBlank @Size(min = 3, max = 120) String name,
        @Size(max = 500) String description,
        @NotNull UUID tournamentId
) {
}
