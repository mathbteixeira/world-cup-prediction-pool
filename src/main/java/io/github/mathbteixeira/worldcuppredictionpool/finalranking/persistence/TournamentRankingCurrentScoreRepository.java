package io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.finalranking.domain.TournamentRankingCurrentScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TournamentRankingCurrentScoreRepository extends JpaRepository<TournamentRankingCurrentScore, UUID> {

    Optional<TournamentRankingCurrentScore> findByPredictionId(UUID predictionId);

    @Modifying
    void deleteByPredictionIdIn(Collection<UUID> predictionIds);

    @Modifying
    void deleteByPoolId(UUID poolId);

    @Query("""
            select cs.pool.id as poolId, cs.user.id as userId, sum(cs.pointsAwarded) as totalPoints
            from TournamentRankingCurrentScore cs
            where cs.pool.id in :poolIds
            group by cs.pool.id, cs.user.id
            """)
    List<PoolUserTotalProjection> aggregateTotalsByPoolAndUser(Collection<UUID> poolIds);

    interface PoolUserTotalProjection {
        UUID getPoolId();

        UUID getUserId();

        Long getTotalPoints();
    }
}