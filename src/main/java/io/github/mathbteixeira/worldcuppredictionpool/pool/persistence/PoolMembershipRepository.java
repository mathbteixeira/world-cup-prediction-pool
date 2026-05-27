package io.github.mathbteixeira.worldcuppredictionpool.pool.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PoolMembershipRepository extends JpaRepository<PoolMembership, UUID> {

    List<PoolMembership> findAllByUserEmailOrderByCreatedAtDesc(String email);

    List<PoolMembership> findAllByPoolIdIn(List<UUID> poolIds);

    Optional<PoolMembership> findByPoolIdAndUserId(UUID poolId, UUID userId);
}
