package io.github.mathbteixeira.worldcuppredictionpool.finalranking.api;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TeamSummaryResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The final-ranking prediction view inside a pool: the tournament's teams (for
 * the podium picker), the prediction deadline, the user's current podium pick
 * (if any) and the official ranking (if confirmed).
 */
public record TournamentRankingResponse(
        UUID poolId,
        UUID tournamentId,
        List<TeamSummaryResponse> teams,
        Instant predictionDeadline,
        boolean predictionOpen,
        TournamentRankingPicks predicted,
        Instant predictionSubmittedAt,
        boolean officialRankingConfirmed,
        TournamentRankingPicks official
) {
}