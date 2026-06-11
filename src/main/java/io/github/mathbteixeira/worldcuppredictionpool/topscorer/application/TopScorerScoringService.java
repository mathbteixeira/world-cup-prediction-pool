package io.github.mathbteixeira.worldcuppredictionpool.topscorer.application;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.PoolLeaderboardRecalculationService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.ScoringRuleResolver;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.StandingsRecalculationResult;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoringRuleDefinition;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.TopScorerScoreBreakdown;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.TopScorerScoringEngine;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.api.AdminTopScorerPredictionResponse;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain.TopScorerCurrentScore;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain.TopScorerPrediction;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence.TopScorerCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence.TopScorerPredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence.TopScorerScoreEventRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TeamSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TournamentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class TopScorerScoringService {

    private final TournamentRepository tournamentRepository;
    private final TopScorerPredictionRepository topScorerPredictionRepository;
    private final TopScorerScoreEventRepository topScorerScoreEventRepository;
    private final TopScorerCurrentScoreRepository topScorerCurrentScoreRepository;
    private final TopScorerScoringEngine topScorerScoringEngine;
    private final ScoringRuleResolver scoringRuleResolver;
    private final PoolLeaderboardRecalculationService poolLeaderboardRecalculationService;
    private final Clock clock;

    public TopScorerScoringService(TournamentRepository tournamentRepository,
                                   TopScorerPredictionRepository topScorerPredictionRepository,
                                   TopScorerScoreEventRepository topScorerScoreEventRepository,
                                   TopScorerCurrentScoreRepository topScorerCurrentScoreRepository,
                                   TopScorerScoringEngine topScorerScoringEngine,
                                   ScoringRuleResolver scoringRuleResolver,
                                   PoolLeaderboardRecalculationService poolLeaderboardRecalculationService,
                                   Clock clock) {
        this.tournamentRepository = tournamentRepository;
        this.topScorerPredictionRepository = topScorerPredictionRepository;
        this.topScorerScoreEventRepository = topScorerScoreEventRepository;
        this.topScorerCurrentScoreRepository = topScorerCurrentScoreRepository;
        this.topScorerScoringEngine = topScorerScoringEngine;
        this.scoringRuleResolver = scoringRuleResolver;
        this.poolLeaderboardRecalculationService = poolLeaderboardRecalculationService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdminTopScorerPredictionResponse> listPredictions(UUID tournamentId) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found"));
        return topScorerPredictionRepository.findAllByTournamentId(tournamentId).stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public StandingsRecalculationResult validateAndRecalculate(UUID predictionId, boolean playerCorrect, boolean goalsCorrect) {
        TopScorerPrediction prediction = topScorerPredictionRepository.findWithDetailsById(predictionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Top-scorer prediction not found"));
        Instant now = Instant.now(clock);
        UUID tournamentId = prediction.getTournament().getId();
        String checksum = checksumFor(predictionId, playerCorrect, goalsCorrect);
        ScoringRuleDefinition rule = scoringRuleResolver.resolve(tournamentId);
        TopScorerScoreBreakdown breakdown = topScorerScoringEngine.score(playerCorrect, goalsCorrect, rule);

        int insertedEvents = topScorerScoreEventRepository.insertIgnoreConflict(
                UUID.randomUUID(),
                now,
                now,
                prediction.getPool().getId(),
                prediction.getUser().getId(),
                tournamentId,
                prediction.getId(),
                breakdown.totalPoints(),
                breakdown.playerPointsAwarded(),
                breakdown.goalsPointsAwarded(),
                playerCorrect,
                goalsCorrect,
                breakdown.explanation(),
                rule.version(),
                checksum,
                now);

        topScorerCurrentScoreRepository.findByPredictionId(prediction.getId())
                .map(existing -> {
                    existing.updateScore(breakdown.totalPoints(), playerCorrect, goalsCorrect, rule.version(), checksum, now);
                    return topScorerCurrentScoreRepository.save(existing);
                })
                .orElseGet(() -> topScorerCurrentScoreRepository.save(new TopScorerCurrentScore(
                        prediction,
                        prediction.getPool(),
                        prediction.getUser(),
                        breakdown.totalPoints(),
                        playerCorrect,
                        goalsCorrect,
                        rule.version(),
                        checksum,
                        now)));

        poolLeaderboardRecalculationService.rebuild(new ArrayList<>(List.of(prediction.getPool().getId())), now);

        return new StandingsRecalculationResult(checksum, 1, 1, insertedEvents == 0);
    }

    private AdminTopScorerPredictionResponse toAdminResponse(TopScorerPrediction prediction) {
        TopScorerCurrentScore currentScore = topScorerCurrentScoreRepository.findByPredictionId(prediction.getId()).orElse(null);
        return new AdminTopScorerPredictionResponse(
                prediction.getId(),
                prediction.getPool().getId(),
                prediction.getPool().getName(),
                prediction.getUser().getId(),
                prediction.getUser().getUsername(),
                prediction.getUser().getEmail(),
                new TeamSummaryResponse(prediction.getTeam().getId(), prediction.getTeam().getName(), prediction.getTeam().getFifaCode()),
                prediction.getPlayerName(),
                prediction.getPredictedGoals(),
                prediction.getSubmittedAt(),
                currentScore != null,
                currentScore == null ? null : currentScore.isPlayerCorrect(),
                currentScore == null ? null : currentScore.isGoalsCorrect(),
                currentScore == null ? null : currentScore.getPointsAwarded(),
                currentScore == null ? null : currentScore.getRecalculatedAt()
        );
    }

    private String checksumFor(UUID predictionId, boolean playerCorrect, boolean goalsCorrect) {
        String raw = predictionId + "|" + playerCorrect + "|" + goalsCorrect;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 should be available", e);
        }
    }
}
