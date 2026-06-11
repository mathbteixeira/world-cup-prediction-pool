package io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.finalranking.domain.TournamentRankingPrediction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TournamentRankingPredictionRepository extends JpaRepository<TournamentRankingPrediction, UUID> {

    @EntityGraph(attributePaths = {"championTeam", "runnerUpTeam", "thirdPlaceTeam", "fourthPlaceTeam"})
    Optional<TournamentRankingPrediction> findByPoolIdAndUserId(UUID poolId, UUID userId);

    @EntityGraph(attributePaths = {"pool", "user", "championTeam", "runnerUpTeam", "thirdPlaceTeam", "fourthPlaceTeam"})
    List<TournamentRankingPrediction> findAllByTournamentId(UUID tournamentId);

    @Query("select p.id from TournamentRankingPrediction p where p.pool.id = :poolId")
    List<UUID> findIdsByPoolId(@Param("poolId") UUID poolId);

    @Query("select p.id from TournamentRankingPrediction p where p.pool.id = :poolId and p.user.id = :userId")
    List<UUID> findIdsByPoolIdAndUserId(@Param("poolId") UUID poolId, @Param("userId") UUID userId);

    @Modifying
    void deleteByPoolId(UUID poolId);

    @Modifying
    void deleteByPoolIdAndUserId(UUID poolId, UUID userId);
}