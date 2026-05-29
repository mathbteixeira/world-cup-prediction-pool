package io.github.mathbteixeira.worldcuppredictionpool.tournament.api;

import java.util.UUID;

public record TeamSummaryResponse(
        UUID id,
        String name,
        String fifaCode
) {
}
