package io.github.mathbteixeira.worldcuppredictionpool.tournament.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResolveMatchParticipantsRequest(
        @NotNull UUID homeTeamId,
        @NotNull UUID awayTeamId
) {
}
