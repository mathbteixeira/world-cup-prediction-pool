package io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PredictionRepository extends JpaRepository<Prediction, UUID> {

    List<Prediction> findAllByMatchId(UUID matchId);

    Optional<Prediction> findByPoolIdAndMatchIdAndUserId(UUID poolId, UUID matchId, UUID userId);

    @Query("""
            select prediction
            from Prediction prediction
            join fetch prediction.pool pool
            join fetch prediction.user user
            join fetch prediction.match match
            join fetch match.tournament tournament
            join fetch match.homeTeam homeTeam
            join fetch match.awayTeam awayTeam
            where pool.id = :poolId
            order by match.kickoffAt asc
            """)
    List<Prediction> findAllForPoolOrderByKickoffAt(@Param("poolId") UUID poolId);
}
