package io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PredictionRepository extends JpaRepository<Prediction, UUID> {

    List<Prediction> findAllByMatchId(UUID matchId);

    Optional<Prediction> findByPoolIdAndMatchIdAndUserId(UUID poolId, UUID matchId, UUID userId);

    Optional<Prediction> findByPoolIdAndMatchIdAndManagedParticipantId(UUID poolId, UUID matchId, UUID managedParticipantId);

    @Query("select prediction.id from Prediction prediction where prediction.managedParticipant.id = :managedParticipantId")
    List<UUID> findIdsByManagedParticipantId(@Param("managedParticipantId") UUID managedParticipantId);

    @Query("select prediction.id from Prediction prediction where prediction.pool.id = :poolId and prediction.user.id = :userId")
    List<UUID> findIdsByPoolIdAndUserId(@Param("poolId") UUID poolId, @Param("userId") UUID userId);

    @Modifying
    void deleteByManagedParticipantId(UUID managedParticipantId);

    @Modifying
    void deleteByPoolId(UUID poolId);

    @Modifying
    void deleteByPoolIdAndUserId(UUID poolId, UUID userId);

    @Query("""
            select prediction
            from Prediction prediction
            join fetch prediction.pool pool
            left join fetch prediction.user user
            left join fetch prediction.managedParticipant managedParticipant
            join fetch prediction.match match
            join fetch match.tournament tournament
            join fetch match.homeTeam homeTeam
            join fetch match.awayTeam awayTeam
            where pool.id = :poolId
            order by match.kickoffAt asc
            """)
    List<Prediction> findAllForPoolOrderByKickoffAt(@Param("poolId") UUID poolId);
}
