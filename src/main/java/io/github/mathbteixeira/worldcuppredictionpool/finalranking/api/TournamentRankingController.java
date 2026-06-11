package io.github.mathbteixeira.worldcuppredictionpool.finalranking.api;

import io.github.mathbteixeira.worldcuppredictionpool.finalranking.application.TournamentRankingPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pools/{poolId}/final-ranking")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Final ranking predictions", description = "Tournament final top-four (podium) prediction APIs.")
public class TournamentRankingController {

    private final TournamentRankingPredictionService tournamentRankingPredictionService;

    public TournamentRankingController(TournamentRankingPredictionService tournamentRankingPredictionService) {
        this.tournamentRankingPredictionService = tournamentRankingPredictionService;
    }

    @GetMapping
    @Operation(summary = "Get final-ranking prediction", description = "Returns the tournament's teams, the prediction deadline, the current user's podium prediction, and the official final ranking when confirmed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Final-ranking view returned"),
            @ApiResponse(responseCode = "400", description = "Pool is not a tournament pool"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Current user is not a member of the pool"),
            @ApiResponse(responseCode = "404", description = "Pool not found")
    })
    public TournamentRankingResponse getRanking(
            @Parameter(description = "Prediction pool id") @PathVariable UUID poolId,
            Authentication authentication) {
        return tournamentRankingPredictionService.getRanking(poolId, authentication.getName());
    }

    @PutMapping("/prediction")
    @Operation(summary = "Submit or update final-ranking prediction", description = "Creates or replaces the current user's prediction of the teams finishing 1st to 4th, before the tournament's first kickoff.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Final-ranking prediction submitted or updated"),
            @ApiResponse(responseCode = "400", description = "Invalid podium or pool is not a tournament pool"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Current user is not a member of the pool"),
            @ApiResponse(responseCode = "404", description = "Pool not found"),
            @ApiResponse(responseCode = "409", description = "Final-ranking predictions are closed")
    })
    public TournamentRankingResponse submit(
            @Parameter(description = "Prediction pool id") @PathVariable UUID poolId,
            @Valid @RequestBody SubmitTournamentRankingRequest request,
            Authentication authentication) {
        return tournamentRankingPredictionService.submit(
                poolId,
                authentication.getName(),
                new TournamentRankingPicks(
                        request.championTeamId(),
                        request.runnerUpTeamId(),
                        request.thirdPlaceTeamId(),
                        request.fourthPlaceTeamId()
                ));
    }
}