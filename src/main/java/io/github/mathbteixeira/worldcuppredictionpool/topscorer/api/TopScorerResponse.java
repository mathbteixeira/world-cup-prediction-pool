package io.github.mathbteixeira.worldcuppredictionpool.topscorer.api;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TeamSummaryResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TopScorerResponse(
        UUID poolId,
        UUID tournamentId,
        List<TeamSummaryResponse> teams,
        Instant predictionDeadline,
        boolean predictionOpen,
        TopScorerPick predicted,
        Instant predictionSubmittedAt
) {
}
