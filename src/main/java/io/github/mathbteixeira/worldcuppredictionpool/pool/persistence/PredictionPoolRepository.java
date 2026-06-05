package io.github.mathbteixeira.worldcuppredictionpool.pool.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PredictionPoolRepository extends JpaRepository<PredictionPool, UUID> {

    Optional<PredictionPool> findByInviteCodeIgnoreCase(String inviteCode);
}
