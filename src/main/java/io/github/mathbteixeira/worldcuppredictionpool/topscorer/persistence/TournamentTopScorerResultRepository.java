package io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain.TournamentTopScorerResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TournamentTopScorerResultRepository extends JpaRepository<TournamentTopScorerResult, UUID> {
    @EntityGraph(attributePaths = {"player", "player.team"})
    Optional<TournamentTopScorerResult> findByTournamentId(UUID tournamentId);
}
