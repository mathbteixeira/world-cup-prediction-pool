package io.github.mathbteixeira.worldcuppredictionpool.tournament.api;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;

import java.util.UUID;

public record TournamentSummaryResponse(
        UUID tournamentId,
        String name,
        String slug,
        String seasonYear,
        TournamentStatus status
) {
}
