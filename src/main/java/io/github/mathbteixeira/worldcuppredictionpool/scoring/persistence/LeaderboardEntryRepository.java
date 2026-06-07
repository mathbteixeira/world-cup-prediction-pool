package io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.domain.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, UUID> {

    void deleteByPoolIdIn(Collection<UUID> poolIds);

    void deleteByManagedParticipantId(UUID managedParticipantId);

    void deleteByPoolId(UUID poolId);

    List<LeaderboardEntry> findAllByPoolIdOrderByRankPositionAsc(UUID poolId);
}
