package io.github.mathbteixeira.worldcuppredictionpool.tournament.api;

import java.util.UUID;

public record PlayerSummaryResponse(
        UUID id,
        UUID teamId,
        String name,
        int rosterNumber
) {
}
