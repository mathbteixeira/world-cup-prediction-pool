package io.github.mathbteixeira.worldcuppredictionpool.tournament.application;

import java.util.UUID;

public record ResolveMatchParticipantsCommand(
        UUID matchId,
        UUID homeTeamId,
        UUID awayTeamId
) {
}
