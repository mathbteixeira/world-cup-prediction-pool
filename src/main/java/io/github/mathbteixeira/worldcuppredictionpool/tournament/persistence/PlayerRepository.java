package io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {
    List<Player> findAllByTeamIdOrderByNameAsc(UUID teamId);

    List<Player> findAllByTeamIdAndTeamTournamentIdOrderByNameAsc(UUID teamId, UUID tournamentId);

    Optional<Player> findByIdAndTeamTournamentId(UUID playerId, UUID tournamentId);
}
