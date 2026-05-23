package io.github.mathbteixeira.worldcuppredictionpool.scoring.application;

import java.util.UUID;

public record UpsertMatchResultCommand(
        UUID matchId,
        int homeScore,
        int awayScore,
        Integer homePenaltyScore,
        Integer awayPenaltyScore,
        boolean finalResult
) {
}
