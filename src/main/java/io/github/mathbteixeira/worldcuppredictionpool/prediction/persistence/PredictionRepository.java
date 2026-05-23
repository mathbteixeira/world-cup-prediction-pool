package io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PredictionRepository extends JpaRepository<Prediction, UUID> {

    List<Prediction> findAllByMatchId(UUID matchId);

    Optional<Prediction> findByPoolIdAndMatchIdAndUserId(UUID poolId, UUID matchId, UUID userId);
}
