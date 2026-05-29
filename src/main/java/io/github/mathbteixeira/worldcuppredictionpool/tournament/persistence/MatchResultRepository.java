package io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchResultRepository extends JpaRepository<MatchResult, UUID> {

    Optional<MatchResult> findByMatchId(UUID matchId);

    List<MatchResult> findAllByMatchIdIn(Collection<UUID> matchIds);
}
