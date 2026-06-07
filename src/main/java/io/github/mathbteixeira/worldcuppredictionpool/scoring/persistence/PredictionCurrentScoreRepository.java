package io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.domain.PredictionCurrentScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PredictionCurrentScoreRepository extends JpaRepository<PredictionCurrentScore, UUID> {

    Optional<PredictionCurrentScore> findByPredictionId(UUID predictionId);

    @Modifying
    void deleteByPredictionIdIn(Collection<UUID> predictionIds);

    @Modifying
    void deleteByPoolId(UUID poolId);

    @Query("""
            select pcs.pool.id as poolId, pcs.user.id as userId, sum(pcs.pointsAwarded) as totalPoints
            from PredictionCurrentScore pcs
            where pcs.pool.id in :poolIds
              and pcs.user is not null
            group by pcs.pool.id, pcs.user.id
            """)
    List<PoolUserTotalProjection> aggregateTotalsByPoolAndUser(Collection<UUID> poolIds);

    @Query("""
            select pcs.pool.id as poolId, pcs.managedParticipant.id as managedParticipantId, sum(pcs.pointsAwarded) as totalPoints
            from PredictionCurrentScore pcs
            where pcs.pool.id in :poolIds
              and pcs.managedParticipant is not null
            group by pcs.pool.id, pcs.managedParticipant.id
            """)
    List<PoolManagedParticipantTotalProjection> aggregateTotalsByPoolAndManagedParticipant(Collection<UUID> poolIds);

    interface PoolUserTotalProjection {
        UUID getPoolId();

        UUID getUserId();

        Long getTotalPoints();
    }

    interface PoolManagedParticipantTotalProjection {
        UUID getPoolId();

        UUID getManagedParticipantId();

        Long getTotalPoints();
    }
}
