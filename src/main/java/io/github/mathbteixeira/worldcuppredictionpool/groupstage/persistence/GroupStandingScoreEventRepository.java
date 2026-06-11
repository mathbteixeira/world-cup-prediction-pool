package io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.groupstage.domain.GroupStandingScoreEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface GroupStandingScoreEventRepository extends JpaRepository<GroupStandingScoreEvent, UUID> {

    @Modifying
    @Transactional
    @Query(value = """
            insert into group_standing_score_events (
                id,
                created_at,
                updated_at,
                pool_id,
                user_id,
                tournament_id,
                prediction_id,
                group_name,
                points_awarded,
                correct_positions,
                explanation,
                rule_version,
                result_checksum,
                calculated_at
            ) values (
                :id,
                :createdAt,
                :updatedAt,
                :poolId,
                :userId,
                :tournamentId,
                :predictionId,
                :groupName,
                :pointsAwarded,
                :correctPositions,
                :explanation,
                :ruleVersion,
                :resultChecksum,
                :calculatedAt
            ) on conflict (prediction_id, result_checksum) do nothing
            """, nativeQuery = true)
    int insertIgnoreConflict(@Param("id") UUID id,
                             @Param("createdAt") Instant createdAt,
                             @Param("updatedAt") Instant updatedAt,
                             @Param("poolId") UUID poolId,
                             @Param("userId") UUID userId,
                             @Param("tournamentId") UUID tournamentId,
                             @Param("predictionId") UUID predictionId,
                             @Param("groupName") String groupName,
                             @Param("pointsAwarded") int pointsAwarded,
                             @Param("correctPositions") int correctPositions,
                             @Param("explanation") String explanation,
                             @Param("ruleVersion") int ruleVersion,
                             @Param("resultChecksum") String resultChecksum,
                             @Param("calculatedAt") Instant calculatedAt);

    @Modifying
    void deleteByPredictionIdIn(Collection<UUID> predictionIds);

    @Modifying
    void deleteByPoolId(UUID poolId);
}