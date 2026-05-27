package io.github.mathbteixeira.worldcuppredictionpool.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ReadmeArchitectureTest {

    @Test
    void shouldDocumentArchitectureAndMilestones() throws IOException {
        String readme = Files.readString(Path.of("README.md"));

        assertThat(readme)
                .contains("## Domain model")
                .contains("### Aggregate boundaries")
                .contains("## Scoring engine")
                .contains("## Recalculation flow (result upsert)")
                .contains("## Tradeoffs and future improvements");
    }
}
