package io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.groupstage.domain.GroupStandingPrediction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupStandingPredictionRepository extends JpaRepository<GroupStandingPrediction, UUID> {

    @EntityGraph(attributePaths = {"firstPlaceTeam", "secondPlaceTeam", "thirdPlaceTeam", "fourthPlaceTeam"})
    Optional<GroupStandingPrediction> findByPoolIdAndUserIdAndGroupName(UUID poolId, UUID userId, String groupName);

    @EntityGraph(attributePaths = {"firstPlaceTeam", "secondPlaceTeam", "thirdPlaceTeam", "fourthPlaceTeam"})
    List<GroupStandingPrediction> findAllByPoolIdAndUserIdOrderByGroupNameAsc(UUID poolId, UUID userId);

    @EntityGraph(attributePaths = {"pool", "user", "firstPlaceTeam", "secondPlaceTeam", "thirdPlaceTeam", "fourthPlaceTeam"})
    List<GroupStandingPrediction> findAllByTournamentIdAndGroupName(UUID tournamentId, String groupName);

    @Query("select p.id from GroupStandingPrediction p where p.pool.id = :poolId")
    List<UUID> findIdsByPoolId(@Param("poolId") UUID poolId);

    @Query("select p.id from GroupStandingPrediction p where p.pool.id = :poolId and p.user.id = :userId")
    List<UUID> findIdsByPoolIdAndUserId(@Param("poolId") UUID poolId, @Param("userId") UUID userId);

    @Modifying
    void deleteByPoolId(UUID poolId);

    @Modifying
    void deleteByPoolIdAndUserId(UUID poolId, UUID userId);
}