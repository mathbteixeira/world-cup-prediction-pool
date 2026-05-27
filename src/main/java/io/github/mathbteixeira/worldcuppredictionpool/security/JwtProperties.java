package io.github.mathbteixeira.worldcuppredictionpool.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
@Validated
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank String secret,
        @NotNull Duration accessTokenTtl
) {

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured via APP_JWT_SECRET or a local/dev profile default");
        }
    }
}
