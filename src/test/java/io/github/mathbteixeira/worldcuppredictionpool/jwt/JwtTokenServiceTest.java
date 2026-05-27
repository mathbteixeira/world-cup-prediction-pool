package io.github.mathbteixeira.worldcuppredictionpool.jwt;

import io.github.mathbteixeira.worldcuppredictionpool.security.JwtProperties;
import io.github.mathbteixeira.worldcuppredictionpool.security.JwtTokenService;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private static final String NON_PRODUCTION_SECRET = "TE9DQUwtREVWLU5PTi1QUk9EVUNUSU9OLUpXVC1TRUNSRVQtQ0hBTkdFLUlOLVBST0QtMjAyNg==";

    @Test
    void shouldGenerateAndParseJwtSubject() {
        JwtTokenService service = new JwtTokenService(new JwtProperties(
                "world-cup-prediction-pool",
                NON_PRODUCTION_SECRET,
                Duration.ofHours(2)
        ));
        UserAccount user = new UserAccount("demo", "demo@example.com", "encoded", UserRole.USER);

        String token = service.generateAccessToken(user);

        assertThat(service.extractSubject(token)).isEqualTo("demo@example.com");
    }
}
