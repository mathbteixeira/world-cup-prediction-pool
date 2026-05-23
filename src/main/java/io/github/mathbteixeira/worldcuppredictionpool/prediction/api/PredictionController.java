package io.github.mathbteixeira.worldcuppredictionpool.prediction.api;

import io.github.mathbteixeira.worldcuppredictionpool.prediction.application.PredictionSubmissionService;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.application.SubmitPredictionCommand;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pools/{poolId}/matches/{matchId}/predictions")
@SecurityRequirement(name = "bearerAuth")
public class PredictionController {

    private final PredictionSubmissionService predictionSubmissionService;

    public PredictionController(PredictionSubmissionService predictionSubmissionService) {
        this.predictionSubmissionService = predictionSubmissionService;
    }

    @PostMapping
    public PredictionResponse submit(@PathVariable UUID poolId,
                                     @PathVariable UUID matchId,
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
                prediction.getUser().getId(),
                prediction.getPredictedHomeScore(),
                prediction.getPredictedAwayScore(),
                prediction.getSubmittedAt()
        );
    }
}
