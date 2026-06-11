package io.github.mathbteixeira.worldcuppredictionpool.topscorer.api;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.StandingsRecalculationResult;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.application.TopScorerScoringService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tournaments/{tournamentId}/top-scorer")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin top scorer", description = "Administrative confirmation of the official tournament top scorer.")
public class AdminTopScorerController {

    private final TopScorerScoringService topScorerScoringService;

    public AdminTopScorerController(TopScorerScoringService topScorerScoringService) {
        this.topScorerScoringService = topScorerScoringService;
    }

    @GetMapping("/predictions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminTopScorerPredictionResponse> listPredictions(@PathVariable UUID tournamentId) {
        return topScorerScoringService.listPredictions(tournamentId);
    }

    @PutMapping("/predictions/{predictionId}/validation")
    @PreAuthorize("hasRole('ADMIN')")
    public TopScorerRecalculationResponse validate(@PathVariable UUID tournamentId,
                                                   @PathVariable UUID predictionId,
                                                   @Valid @RequestBody TopScorerValidationRequest request) {
        StandingsRecalculationResult result = topScorerScoringService.validateAndRecalculate(
                predictionId,
                request.playerCorrect(),
                request.goalsCorrect());
        return new TopScorerRecalculationResponse(
                tournamentId,
                result.resultChecksum(),
                result.scoredPredictions(),
                result.affectedPools(),
                result.idempotentReplay()
        );
    }
}
