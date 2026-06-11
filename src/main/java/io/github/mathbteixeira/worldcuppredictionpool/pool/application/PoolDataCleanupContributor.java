package io.github.mathbteixeira.worldcuppredictionpool.pool.application;

import java.util.UUID;

/**
 * Lets each prediction feature purge its own pool-scoped data when a pool is
 * deleted or a member is removed. Implementations are discovered as beans and
 * invoked by {@code PoolService} and {@code AdminPoolModerationService}, so new
 * prediction types can hook into existing cleanup flows without those services
 * having to know about every feature's tables.
 *
 * <p>Implementations must delete in FK-safe order (dependent score/audit rows
 * before the predictions they reference).
 */
public interface PoolDataCleanupContributor {

    /** Removes all of this feature's data for the given pool. */
    void deletePoolData(UUID poolId);

    /** Removes this feature's data for a single user within the given pool. */
    void deleteUserPoolData(UUID poolId, UUID userId);
}