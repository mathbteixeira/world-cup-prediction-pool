package io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.domain.PredictionCurrentScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PredictionCurrentScoreRepository extends JpaRepository<PredictionCurrentScore, UUID> {

    Optional<PredictionCurrentScore> findByPredictionId(UUID predictionId);

    @Query("""
            select pcs.pool.id as poolId, pcs.user.id as userId, sum(pcs.pointsAwarded) as totalPoints
            from PredictionCurrentScore pcs
            where pcs.pool.id in :poolIds
            group by pcs.pool.id, pcs.user.id
            """)
    List<PoolUserTotalProjection> aggregateTotalsByPoolAndUser(Collection<UUID> poolIds);

    interface PoolUserTotalProjection {
        UUID getPoolId();

        UUID getUserId();

        Long getTotalPoints();
    }
}
