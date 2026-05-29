package io.github.mathbteixeira.worldcuppredictionpool.scoring.api;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.MatchResultScoringService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.RecalculationResult;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.UpsertMatchResultCommand;
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
@RequestMapping("/api/v1/admin/matches/{matchId}/result")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin match results", description = "Administrative match result upsert and scoring recalculation APIs.")
public class AdminMatchResultController {

    private final MatchResultScoringService matchResultScoringService;

    public AdminMatchResultController(MatchResultScoringService matchResultScoringService) {
        this.matchResultScoringService = matchResultScoringService;
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upsert match result", description = "Creates or updates the official result for a match and recalculates affected predictions and leaderboards.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Result upserted and scoring recalculated"),
            @ApiResponse(responseCode = "400", description = "Invalid result request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Admin role required"),
            @ApiResponse(responseCode = "404", description = "Match not found")
    })
    public MatchResultRecalculationResponse upsertResult(@Parameter(description = "Match id") @PathVariable UUID matchId,
                                                         @Valid @RequestBody UpsertMatchResultRequest request) {
        RecalculationResult result = matchResultScoringService.upsertResultAndRecalculate(new UpsertMatchResultCommand(
                matchId,
                request.homeScore(),
                request.awayScore(),
                request.homePenaltyScore(),
                request.awayPenaltyScore(),
                request.finalResult()
        ));

        return new MatchResultRecalculationResponse(
                result.matchId(),
                request.homeScore(),
                request.awayScore(),
                request.homePenaltyScore(),
                request.awayPenaltyScore(),
                request.finalResult(),
                result.resultChecksum(),
                result.scoredPredictions(),
                result.affectedPools(),
                result.idempotentReplay()
        );
    }
}
