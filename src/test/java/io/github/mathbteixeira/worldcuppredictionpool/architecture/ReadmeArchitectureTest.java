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
                .contains("## Proposed architecture")
                .contains("### Package layout")
                .contains("### Aggregates and boundaries")
                .contains("### Database tables")
                .contains("### API surface")
                .contains("## First implementation milestones");
    }
}
