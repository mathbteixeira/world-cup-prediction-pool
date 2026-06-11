package io.github.mathbteixeira.worldcuppredictionpool.groupstage.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A group as seen from inside a pool: its teams, the prediction deadline, the
 * current user's prediction (if any) and the official standings (if confirmed).
 * Team-id lists are ordered from 1st to 4th place.
 */
public record GroupStandingResponse(
        UUID poolId,
        UUID tournamentId,
        String groupName,
        List<GroupTeamResponse> teams,
        Instant predictionDeadline,
        boolean predictionOpen,
        List<UUID> predictedTeamIdsByPosition,
        Instant predictionSubmittedAt,
        boolean officialStandingConfirmed,
        List<UUID> officialTeamIdsByPosition
) {
}