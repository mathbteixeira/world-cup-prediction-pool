package io.github.mathbteixeira.worldcuppredictionpool.jwt;

import io.github.mathbteixeira.worldcuppredictionpool.security.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void shouldRequireSecretForNonLocalProfiles() {
        assertThatThrownBy(() -> new JwtProperties(
                "world-cup-prediction-pool",
                "",
                Duration.ofHours(2)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("APP_JWT_SECRET must be configured for non-local profiles");
    }
}
