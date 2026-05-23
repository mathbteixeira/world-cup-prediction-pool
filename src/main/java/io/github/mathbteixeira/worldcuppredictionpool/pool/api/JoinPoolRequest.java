package io.github.mathbteixeira.worldcuppredictionpool.pool.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinPoolRequest(
        @NotBlank @Size(min = 6, max = 20) String inviteCode
) {
}
