package io.github.mathbteixeira.worldcuppredictionpool.scoring.application;

/**
 * Outcome of confirming an official standings/ranking snapshot and recalculating
 * the derived scores and leaderboards. Shared by the group-position and
 * tournament final-ranking confirmation flows.
 *
 * @param resultChecksum     checksum of the confirmed snapshot
 * @param scoredPredictions  number of predictions (re)scored
 * @param affectedPools      number of pools whose leaderboard was rebuilt
 * @param idempotentReplay   true when the confirmed snapshot matched the
 *                           previous one, so no new score events were written
 */
public record StandingsRecalculationResult(
        String resultChecksum,
        int scoredPredictions,
        int affectedPools,
        boolean idempotentReplay
) {
}