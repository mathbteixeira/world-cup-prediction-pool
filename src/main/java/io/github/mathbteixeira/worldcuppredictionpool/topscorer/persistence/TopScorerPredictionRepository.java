package io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain.TopScorerPrediction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopScorerPredictionRepository extends JpaRepository<TopScorerPrediction, UUID> {
    @EntityGraph(attributePaths = {"team"})
    Optional<TopScorerPrediction> findByPoolIdAndUserId(UUID poolId, UUID userId);

    @EntityGraph(attributePaths = {"pool", "user", "team"})
    List<TopScorerPrediction> findAllByTournamentId(UUID tournamentId);

    @EntityGraph(attributePaths = {"pool", "user", "team", "tournament"})
    @Query("select p from TopScorerPrediction p where p.id = :predictionId")
    Optional<TopScorerPrediction> findWithDetailsById(UUID predictionId);

    @Query("select p.id from TopScorerPrediction p where p.pool.id = :poolId and p.user.id = :userId")
    List<UUID> findIdsByPoolIdAndUserId(UUID poolId, UUID userId);

    @Modifying
    void deleteByPoolId(UUID poolId);

    @Modifying
    void deleteByPoolIdAndUserId(UUID poolId, UUID userId);
}
