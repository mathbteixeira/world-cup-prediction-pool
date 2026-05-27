package io.github.mathbteixeira.worldcuppredictionpool.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TokenRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
