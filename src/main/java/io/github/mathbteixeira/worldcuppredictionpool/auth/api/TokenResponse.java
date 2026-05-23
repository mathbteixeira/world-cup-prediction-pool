package io.github.mathbteixeira.worldcuppredictionpool.auth.api;

public record TokenResponse(
        String accessToken,
        String tokenType,
        String username,
        String email,
        String role
) {
}
