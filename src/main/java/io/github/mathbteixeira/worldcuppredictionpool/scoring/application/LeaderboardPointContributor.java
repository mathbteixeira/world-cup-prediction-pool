package io.github.mathbteixeira.worldcuppredictionpool.scoring.application;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Supplies the points a prediction type contributes to pool leaderboards.
 *
 * <p>Each prediction feature (match score predictions, group-position
 * predictions, tournament final-ranking predictions, ...) provides its own
 * implementation. {@link PoolLeaderboardRecalculationService} sums every
 * contributor so a pool's leaderboard always reflects all prediction types,
 * regardless of which recalculation trigger fired. New prediction types can be
 * added by publishing another contributor bean without touching the rebuild
 * logic.
 */
public interface LeaderboardPointContributor {

    /**
     * @param poolIds the pools being rebuilt
     * @return the per-participant point contributions within those pools;
     * participants with no contribution may be omitted.
     */
    List<ParticipantPoints> contributionsFor(Collection<UUID> poolIds);
}