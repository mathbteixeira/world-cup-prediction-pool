package io.github.mathbteixeira.worldcuppredictionpool.finalranking.api;

import io.github.mathbteixeira.worldcuppredictionpool.finalranking.application.TournamentRankingScoringService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.StandingsRecalculationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/v1/admin/tournaments/{tournamentId}/final-ranking")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin final ranking", description = "Administrative confirmation of the official tournament final ranking and scoring recalculation.")
public class AdminTournamentRankingController {

    private final TournamentRankingScoringService tournamentRankingScoringService;

    public AdminTournamentRankingController(TournamentRankingScoringService tournamentRankingScoringService) {
        this.tournamentRankingScoringService = tournamentRankingScoringService;
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Confirm official final ranking", description = "Confirms the tournament's final top-four ranking and recalculates the affected podium predictions and pool leaderboards. This is the endpoint behind the 'confirm final ranking' admin action.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Final ranking confirmed and scoring recalculated"),
            @ApiResponse(responseCode = "400", description = "Invalid podium"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Admin role required"),
            @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public TournamentRankingRecalculationResponse confirmRanking(
            @Parameter(description = "Tournament id") @PathVariable UUID tournamentId,
            @Valid @RequestBody ConfirmTournamentRankingRequest request) {
        StandingsRecalculationResult result = tournamentRankingScoringService.confirmAndRecalculate(
                tournamentId,
                request.championTeamId(),
                request.runnerUpTeamId(),
                request.thirdPlaceTeamId(),
                request.fourthPlaceTeamId());
        return new TournamentRankingRecalculationResponse(
                tournamentId,
                new TournamentRankingPicks(
                        request.championTeamId(),
                        request.runnerUpTeamId(),
                        request.thirdPlaceTeamId(),
                        request.fourthPlaceTeamId()
                ),
                result.resultChecksum(),
                result.scoredPredictions(),
                result.affectedPools(),
                result.idempotentReplay()
        );
    }
}