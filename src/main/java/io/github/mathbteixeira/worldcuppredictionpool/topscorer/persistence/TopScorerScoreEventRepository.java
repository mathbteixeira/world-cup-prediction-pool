package io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain.TopScorerScoreEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface TopScorerScoreEventRepository extends JpaRepository<TopScorerScoreEvent, UUID> {

    @Modifying
    @Transactional
    @Query(value = """
            insert into top_scorer_score_events (
                id,
                created_at,
                updated_at,
                pool_id,
                user_id,
                tournament_id,
                prediction_id,
                points_awarded,
                player_points_awarded,
                goals_points_awarded,
                player_correct,
                goals_correct,
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
                :pointsAwarded,
                :playerPointsAwarded,
                :goalsPointsAwarded,
                :playerCorrect,
                :goalsCorrect,
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
                             @Param("pointsAwarded") int pointsAwarded,
                             @Param("playerPointsAwarded") int playerPointsAwarded,
                             @Param("goalsPointsAwarded") int goalsPointsAwarded,
                             @Param("playerCorrect") boolean playerCorrect,
                             @Param("goalsCorrect") boolean goalsCorrect,
                             @Param("explanation") String explanation,
                             @Param("ruleVersion") int ruleVersion,
                             @Param("resultChecksum") String resultChecksum,
                             @Param("calculatedAt") Instant calculatedAt);

    @Modifying
    void deleteByPredictionIdIn(Collection<UUID> predictionIds);

    @Modifying
    void deleteByPoolId(UUID poolId);
}
