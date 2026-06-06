package io.github.mathbteixeira.worldcuppredictionpool.pool.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.ManagedParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManagedParticipantRepository extends JpaRepository<ManagedParticipant, UUID> {

    List<ManagedParticipant> findAllByPoolIdOrderByDisplayNameAsc(UUID poolId);

    List<ManagedParticipant> findAllByPoolIdIn(Iterable<UUID> poolIds);

    Optional<ManagedParticipant> findByPoolIdAndId(UUID poolId, UUID participantId);

    boolean existsByPoolIdAndDisplayNameIgnoreCase(UUID poolId, String displayName);
}
