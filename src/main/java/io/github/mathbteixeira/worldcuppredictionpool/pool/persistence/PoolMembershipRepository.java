package io.github.mathbteixeira.worldcuppredictionpool.pool.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PoolMembershipRepository extends JpaRepository<PoolMembership, UUID> {

    List<PoolMembership> findAllByUserEmailOrderByCreatedAtDesc(String email);

    List<PoolMembership> findAllByPoolIdIn(List<UUID> poolIds);

    List<PoolMembership> findAllByPoolId(UUID poolId);

    Optional<PoolMembership> findByPoolIdAndUserId(UUID poolId, UUID userId);

    long countByPoolId(UUID poolId);

    void deleteByPoolIdAndUserId(UUID poolId, UUID userId);

    void deleteByPoolId(UUID poolId);
}
