package io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.finalranking.domain.TournamentRankingScoreEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface TournamentRankingScoreEventRepository extends JpaRepository<TournamentRankingScoreEvent, UUID> {

    @Modifying
    @Transactional
    @Query(value = """
            insert into tournament_ranking_score_events (
                id,
                created_at,
                updated_at,
                pool_id,
                user_id,
                tournament_id,
                prediction_id,
                points_awarded,
                champion_points_awarded,
                runner_up_points_awarded,
                third_place_points_awarded,
                fourth_place_points_awarded,
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
                :championPointsAwarded,
                :runnerUpPointsAwarded,
                :thirdPlacePointsAwarded,
                :fourthPlacePointsAwarded,
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
                             @Param("championPointsAwarded") int championPointsAwarded,
                             @Param("runnerUpPointsAwarded") int runnerUpPointsAwarded,
                             @Param("thirdPlacePointsAwarded") int thirdPlacePointsAwarded,
                             @Param("fourthPlacePointsAwarded") int fourthPlacePointsAwarded,
                             @Param("explanation") String explanation,
                             @Param("ruleVersion") int ruleVersion,
                             @Param("resultChecksum") String resultChecksum,
                             @Param("calculatedAt") Instant calculatedAt);

    @Modifying
    void deleteByPredictionIdIn(Collection<UUID> predictionIds);

    @Modifying
    void deleteByPoolId(UUID poolId);
}