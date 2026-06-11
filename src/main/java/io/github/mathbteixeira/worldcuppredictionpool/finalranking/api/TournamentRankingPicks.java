package io.github.mathbteixeira.worldcuppredictionpool.finalranking.api;

import java.util.UUID;

/** The four podium team ids: champion (1st), runner-up (2nd), 3rd and 4th. */
public record TournamentRankingPicks(
        UUID championTeamId,
        UUID runnerUpTeamId,
        UUID thirdPlaceTeamId,
        UUID fourthPlaceTeamId
) {
}