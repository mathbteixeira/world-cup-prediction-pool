package io.github.mathbteixeira.worldcuppredictionpool.finalranking.api;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * A user's podium prediction: the four teams expected to finish 1st to 4th.
 * All four must be distinct teams of the pool's tournament.
 */
public record SubmitTournamentRankingRequest(
        @NotNull UUID championTeamId,
        @NotNull UUID runnerUpTeamId,
        @NotNull UUID thirdPlaceTeamId,
        @NotNull UUID fourthPlaceTeamId
) {
}