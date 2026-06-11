package io.github.mathbteixeira.worldcuppredictionpool.topscorer.api;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TeamSummaryResponse;

import java.time.Instant;
import java.util.UUID;

public record AdminTopScorerPredictionResponse(
        UUID predictionId,
        UUID poolId,
        String poolName,
        UUID userId,
        String username,
        String email,
        TeamSummaryResponse team,
        String playerName,
        int predictedGoals,
        Instant submittedAt,
        boolean validated,
        Boolean playerCorrect,
        Boolean goalsCorrect,
        Integer pointsAwarded,
        Instant validatedAt
) {
}
