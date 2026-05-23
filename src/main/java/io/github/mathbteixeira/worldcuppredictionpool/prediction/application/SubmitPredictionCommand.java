package io.github.mathbteixeira.worldcuppredictionpool.prediction.application;

import java.util.UUID;

public record SubmitPredictionCommand(
        UUID poolId,
        UUID matchId,
        String userEmail,
        int homeScore,
        int awayScore
) {
}
