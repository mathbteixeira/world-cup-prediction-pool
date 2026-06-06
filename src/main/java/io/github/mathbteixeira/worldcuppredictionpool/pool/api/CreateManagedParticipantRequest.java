package io.github.mathbteixeira.worldcuppredictionpool.pool.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateManagedParticipantRequest(
        @NotBlank @Size(max = 80) String name
) {
}
