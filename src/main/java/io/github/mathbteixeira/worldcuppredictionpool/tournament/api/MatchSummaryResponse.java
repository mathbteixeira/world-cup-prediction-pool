package io.github.mathbteixeira.worldcuppredictionpool.tournament.api;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;

import java.time.Instant;
import java.util.UUID;

public record MatchSummaryResponse(
        UUID matchId,
        UUID tournamentId,
        TeamSummaryResponse homeTeam,
        TeamSummaryResponse awayTeam,
        String homePlaceholder,
        String awayPlaceholder,
        Instant kickoffAt,
        String stage,
        String groupName,
        MatchStatus status,
        MatchResultResponse result,
        boolean predictionOpen
) {
}
