package io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.domain.ScoreEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ScoreEventRepository extends JpaRepository<ScoreEvent, UUID> {

    @Modifying
    @Query(value = """
            insert into score_events (
                id,
                created_at,
                updated_at,
                pool_id,
                user_id,
                match_id,
                prediction_id,
                points_awarded,
                exact_score_points_awarded,
                outcome_points_awarded,
                goal_difference_bonus_points_awarded,
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
                :matchId,
                :predictionId,
                :pointsAwarded,
                :exactScorePointsAwarded,
                :outcomePointsAwarded,
                :goalDifferenceBonusPointsAwarded,
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
                             @Param("matchId") UUID matchId,
                             @Param("predictionId") UUID predictionId,
                             @Param("pointsAwarded") int pointsAwarded,
                             @Param("exactScorePointsAwarded") int exactScorePointsAwarded,
                             @Param("outcomePointsAwarded") int outcomePointsAwarded,
                             @Param("goalDifferenceBonusPointsAwarded") int goalDifferenceBonusPointsAwarded,
                             @Param("explanation") String explanation,
                             @Param("ruleVersion") int ruleVersion,
                             @Param("resultChecksum") String resultChecksum,
                             @Param("calculatedAt") Instant calculatedAt);
}
