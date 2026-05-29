package io.github.mathbteixeira.worldcuppredictionpool.prediction.api;

import io.github.mathbteixeira.worldcuppredictionpool.prediction.application.PredictionSubmissionService;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.application.SubmitPredictionCommand;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pools/{poolId}")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Predictions", description = "Prediction submission and lookup APIs.")
public class PredictionController {

    private final PredictionSubmissionService predictionSubmissionService;

    public PredictionController(PredictionSubmissionService predictionSubmissionService) {
        this.predictionSubmissionService = predictionSubmissionService;
    }

    @GetMapping("/predictions")
    @Operation(summary = "List current user predictions", description = "Returns the authenticated user's submitted predictions for the pool, sorted by match kickoff time.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user predictions returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Current user is not a member of the pool"),
            @ApiResponse(responseCode = "404", description = "Pool not found")
    })
    public List<UserPredictionResponse> listCurrentUserPredictions(
            @Parameter(description = "Prediction pool id") @PathVariable UUID poolId,
            Authentication authentication) {
        return predictionSubmissionService.listCurrentUserPredictions(poolId, authentication.getName());
    }

    @PutMapping("/matches/{matchId}/prediction")
    @Operation(summary = "Submit or update prediction", description = "Creates or replaces the current user's score prediction for a match before kickoff.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prediction submitted or updated"),
            @ApiResponse(responseCode = "400", description = "Invalid prediction request or prediction window closed"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Current user is not a member of the pool"),
            @ApiResponse(responseCode = "404", description = "Pool or match not found")
    })
    public PredictionResponse submitOrUpdate(@Parameter(description = "Prediction pool id") @PathVariable UUID poolId,
                                             @Parameter(description = "Match id") @PathVariable UUID matchId,
                                             @Valid @RequestBody SubmitPredictionRequest request,
                                             Authentication authentication) {
        Prediction prediction = predictionSubmissionService.submit(new SubmitPredictionCommand(
                poolId,
                matchId,
                authentication.getName(),
                request.homeScore(),
                request.awayScore()
        ));
        return new PredictionResponse(
                prediction.getId(),
                prediction.getPool().getId(),
                prediction.getMatch().getId(),
                prediction.getPredictedHomeScore(),
                prediction.getPredictedAwayScore(),
                prediction.getSubmittedAt()
        );
    }
}
