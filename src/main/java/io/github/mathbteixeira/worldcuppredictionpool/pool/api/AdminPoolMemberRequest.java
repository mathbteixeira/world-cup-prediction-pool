package io.github.mathbteixeira.worldcuppredictionpool.pool.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminPoolMemberRequest(
        @NotBlank @Email String email
) {
}
