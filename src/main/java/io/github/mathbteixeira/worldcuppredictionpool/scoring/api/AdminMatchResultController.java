package io.github.mathbteixeira.worldcuppredictionpool.scoring.api;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.MatchResultScoringService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.RecalculationResult;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.UpsertMatchResultCommand;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
public class AdminMatchResultController {

    private final MatchResultScoringService matchResultScoringService;

    public AdminMatchResultController(MatchResultScoringService matchResultScoringService) {
        this.matchResultScoringService = matchResultScoringService;
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public MatchResultRecalculationResponse upsertResult(@PathVariable UUID matchId,
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
