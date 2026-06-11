package io.github.mathbteixeira.worldcuppredictionpool.finalranking.api;

import java.util.UUID;

/**
 * Admin response after confirming the official final ranking and recalculating
 * the affected podium predictions and pool leaderboards.
 */
public record TournamentRankingRecalculationResponse(
        UUID tournamentId,
        TournamentRankingPicks official,
        String resultChecksum,
        int scoredPredictions,
        int affectedPools,
        boolean idempotentReplay
) {
}