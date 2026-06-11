package io.github.mathbteixeira.worldcuppredictionpool.finalranking.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Admin confirmation of the official final top-four ranking of a tournament.
 * All four team ids must be distinct and belong to the tournament.
 */
public record ConfirmTournamentRankingRequest(
        @NotNull UUID championTeamId,
        @NotNull UUID runnerUpTeamId,
        @NotNull UUID thirdPlaceTeamId,
        @NotNull UUID fourthPlaceTeamId
) {
}