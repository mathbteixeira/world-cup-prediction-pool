package io.github.mathbteixeira.worldcuppredictionpool.scoring.application;

import java.util.UUID;

/**
 * A single participant's point contribution inside one pool, produced by a
 * {@link LeaderboardPointContributor}. Exactly one of {@code userId} or
 * {@code managedParticipantId} is expected to be non-null, mirroring the
 * participant duality used across the scoring projections.
 */
public record ParticipantPoints(
        UUID poolId,
        UUID userId,
        UUID managedParticipantId,
        long points
) {
    public static ParticipantPoints forUser(UUID poolId, UUID userId, long points) {
        return new ParticipantPoints(poolId, userId, null, points);
    }

    public static ParticipantPoints forManagedParticipant(UUID poolId, UUID managedParticipantId, long points) {
        return new ParticipantPoints(poolId, null, managedParticipantId, points);
    }
}