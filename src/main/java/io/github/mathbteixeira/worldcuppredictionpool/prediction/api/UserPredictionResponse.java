package io.github.mathbteixeira.worldcuppredictionpool.prediction.api;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.MatchSummaryResponse;

import java.time.Instant;
import java.util.UUID;

public record UserPredictionResponse(
        UUID predictionId,
        UUID poolId,
        MatchSummaryResponse match,
        int homeScore,
        int awayScore,
        Instant submittedAt
) {
}
