package io.github.mathbteixeira.worldcuppredictionpool.jwt;

import io.github.mathbteixeira.worldcuppredictionpool.security.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void shouldRequireConfiguredSecret() {
        assertThatThrownBy(() -> new JwtProperties(
                "world-cup-prediction-pool",
                "",
                Duration.ofHours(2)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT secret must be configured via APP_JWT_SECRET or a local/dev profile default");
    }
}
