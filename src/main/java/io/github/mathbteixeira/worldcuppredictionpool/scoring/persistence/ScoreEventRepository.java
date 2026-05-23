package io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.domain.ScoreEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScoreEventRepository extends JpaRepository<ScoreEvent, UUID> {

    List<ScoreEvent> findAllByPoolIdOrderByCalculatedAtDesc(UUID poolId);

    List<ScoreEvent> findAllByPoolIdAndUserIdOrderByCalculatedAtDesc(UUID poolId, UUID userId);
}
