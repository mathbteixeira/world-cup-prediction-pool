package io.github.mathbteixeira.worldcuppredictionpool.topscorer.api;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.StandingsRecalculationResult;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.application.TopScorerScoringService;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.PlayerRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tournaments/{tournamentId}/top-scorer")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin top scorer", description = "Administrative confirmation of the official tournament top scorer.")
public class AdminTopScorerController {

    private final TopScorerScoringService topScorerScoringService;
    private final PlayerRepository playerRepository;

    public AdminTopScorerController(TopScorerScoringService topScorerScoringService, PlayerRepository playerRepository) {
        this.topScorerScoringService = topScorerScoringService;
        this.playerRepository = playerRepository;
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public TopScorerRecalculationResponse confirm(@PathVariable UUID tournamentId,
                                                  @Valid @RequestBody ConfirmTopScorerRequest request) {
        StandingsRecalculationResult result = topScorerScoringService.confirmAndRecalculate(
                tournamentId,
                request.playerId(),
                request.goals());
        UUID teamId = playerRepository.findByIdAndTeamTournamentId(request.playerId(), tournamentId)
                .map(player -> player.getTeam().getId())
                .orElse(null);
        return new TopScorerRecalculationResponse(
                tournamentId,
                new TopScorerPick(teamId, request.playerId(), request.goals()),
                result.resultChecksum(),
                result.scoredPredictions(),
                result.affectedPools(),
                result.idempotentReplay()
        );
    }
}
