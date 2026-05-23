package io.github.mathbteixeira.worldcuppredictionpool.scoring.api;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.LeaderboardQueryService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.MatchResultScoringService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.RecalculationResult;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.ScoreAuditQueryService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.UpsertMatchResultCommand;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.domain.LeaderboardEntry;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.domain.ScoreEvent;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class ScoringController {

    private final MatchResultScoringService matchResultScoringService;
    private final LeaderboardQueryService leaderboardQueryService;
    private final ScoreAuditQueryService scoreAuditQueryService;

    public ScoringController(MatchResultScoringService matchResultScoringService,
                             LeaderboardQueryService leaderboardQueryService,
                             ScoreAuditQueryService scoreAuditQueryService) {
        this.matchResultScoringService = matchResultScoringService;
        this.leaderboardQueryService = leaderboardQueryService;
        this.scoreAuditQueryService = scoreAuditQueryService;
    }

    @PutMapping("/api/v1/admin/matches/{matchId}/result")
    @PreAuthorize("hasRole('ADMIN')")
    public RecalculationResult upsertResult(@PathVariable UUID matchId, @Valid @RequestBody UpsertMatchResultRequest request) {
        return matchResultScoringService.upsertResultAndRecalculate(new UpsertMatchResultCommand(
                matchId,
                request.homeScore(),
                request.awayScore(),
                request.homePenaltyScore(),
                request.awayPenaltyScore(),
                request.finalResult()
        ));
    }

    @GetMapping("/api/v1/pools/{poolId}/leaderboard")
    public List<LeaderboardEntryResponse> leaderboard(@PathVariable UUID poolId, Authentication authentication) {
        List<LeaderboardEntry> entries = leaderboardQueryService.getByPool(poolId, authentication.getName());
        return entries.stream()
                .map(entry -> new LeaderboardEntryResponse(
                        entry.getUser().getId(),
                        entry.getUser().getUsername(),
                        entry.getTotalPoints(),
                        entry.getRankPosition()
                ))
                .toList();
    }

    @GetMapping("/api/v1/pools/{poolId}/score-audit")
    public List<ScoreEventResponse> audit(@PathVariable UUID poolId,
                                          @RequestParam(required = false) UUID userId,
                                          Authentication authentication) {
        List<ScoreEvent> events = scoreAuditQueryService.getByPool(poolId, userId, authentication.getName());
        return events.stream()
                .map(event -> new ScoreEventResponse(
                        event.getId(),
                        event.getMatch().getId(),
                        event.getPrediction().getId(),
                        event.getUser().getId(),
                        event.getPointsAwarded(),
                        event.getRuleVersion(),
                        event.getResultChecksum(),
                        event.getCreatedAt()
                ))
                .toList();
    }
}
