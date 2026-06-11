package io.github.mathbteixeira.worldcuppredictionpool.tournament.api;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.PlayerRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}/teams/{teamId}/players")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tournament players", description = "Player catalog by national team.")
public class TournamentPlayerController {

    private final PlayerRepository playerRepository;

    public TournamentPlayerController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<PlayerSummaryResponse> listPlayers(@PathVariable UUID tournamentId, @PathVariable UUID teamId) {
        return playerRepository.findAllByTeamIdAndTeamTournamentIdOrderByNameAsc(teamId, tournamentId).stream()
                .map(player -> new PlayerSummaryResponse(
                        player.getId(),
                        player.getTeam().getId(),
                        player.getName(),
                        player.getRosterNumber()))
                .toList();
    }
}
