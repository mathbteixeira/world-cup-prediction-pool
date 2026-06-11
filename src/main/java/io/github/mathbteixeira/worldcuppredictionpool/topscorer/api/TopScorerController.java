package io.github.mathbteixeira.worldcuppredictionpool.topscorer.api;

import io.github.mathbteixeira.worldcuppredictionpool.topscorer.application.TopScorerPredictionService;
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
@RequestMapping("/api/v1/pools/{poolId}/top-scorer")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Top scorer predictions", description = "Tournament top scorer prediction APIs.")
public class TopScorerController {

    private final TopScorerPredictionService topScorerPredictionService;

    public TopScorerController(TopScorerPredictionService topScorerPredictionService) {
        this.topScorerPredictionService = topScorerPredictionService;
    }

    @GetMapping
    public TopScorerResponse getTopScorer(@PathVariable UUID poolId, Authentication authentication) {
        return topScorerPredictionService.getTopScorer(poolId, authentication.getName());
    }

    @PutMapping("/prediction")
    public TopScorerResponse submit(@PathVariable UUID poolId,
                                    @Valid @RequestBody SubmitTopScorerPredictionRequest request,
                                    Authentication authentication) {
        return topScorerPredictionService.submit(poolId, authentication.getName(), request.teamId(), request.playerName(), request.goals());
    }
}
