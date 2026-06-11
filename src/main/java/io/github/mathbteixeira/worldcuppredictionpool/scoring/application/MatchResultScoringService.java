package io.github.mathbteixeira.worldcuppredictionpool.scoring.application;

import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence.PredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.domain.PredictionCurrentScore;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.MatchScoreInput;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.PredictionScoreInput;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.PredictionScoringEngine;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoreBreakdown;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoringRuleDefinition;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.PredictionCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.ScoreEventRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchResult;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchResultRepository;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MatchResultScoringService {

    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;
    private final PredictionRepository predictionRepository;
    private final ScoreEventRepository scoreEventRepository;
    private final PredictionCurrentScoreRepository predictionCurrentScoreRepository;
    private final PoolLeaderboardRecalculationService poolLeaderboardRecalculationService;
    private final PredictionScoringEngine predictionScoringEngine;
    private final ScoringRuleResolver scoringRuleResolver;
    private final Clock clock;

    public MatchResultScoringService(MatchRepository matchRepository,
                                     MatchResultRepository matchResultRepository,
                                     PredictionRepository predictionRepository,
                                     ScoreEventRepository scoreEventRepository,
                                     PredictionCurrentScoreRepository predictionCurrentScoreRepository,
                                     PoolLeaderboardRecalculationService poolLeaderboardRecalculationService,
                                     PredictionScoringEngine predictionScoringEngine,
                                     ScoringRuleResolver scoringRuleResolver,
                                     Clock clock) {
        this.matchRepository = matchRepository;
        this.matchResultRepository = matchResultRepository;
        this.predictionRepository = predictionRepository;
        this.scoreEventRepository = scoreEventRepository;
        this.predictionCurrentScoreRepository = predictionCurrentScoreRepository;
        this.poolLeaderboardRecalculationService = poolLeaderboardRecalculationService;
        this.predictionScoringEngine = predictionScoringEngine;
        this.scoringRuleResolver = scoringRuleResolver;
        this.clock = clock;
    }

    @Transactional
    public RecalculationResult upsertResultAndRecalculate(UpsertMatchResultCommand command) {
        Match match = matchRepository.findById(command.matchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));
        if (!match.hasResolvedTeams()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match participants are not resolved");
        }

        Instant now = Instant.now(clock);
        String checksum = checksumFor(command);

        MatchResult matchResult = matchResultRepository.findByMatchId(command.matchId())
                .map(existing -> {
                    existing.updateResult(
                            command.homeScore(),
                            command.awayScore(),
                            command.homePenaltyScore(),
                            command.awayPenaltyScore(),
                            command.finalResult(),
                            now,
                            checksum
                    );
                    return existing;
                })
                .orElseGet(() -> new MatchResult(
                        match,
                        command.homeScore(),
                        command.awayScore(),
                        command.homePenaltyScore(),
                        command.awayPenaltyScore(),
                        command.finalResult(),
                        now,
                        checksum
                ));

        matchResultRepository.save(matchResult);

        ScoringRuleDefinition rule = scoringRuleResolver.resolve(match.getTournament().getId());
        MatchScoreInput actual = new MatchScoreInput(matchResult.getHomeScore(), matchResult.getAwayScore());
        List<Prediction> predictions = predictionRepository.findAllByMatchId(command.matchId());

        int insertedEvents = 0;
        for (Prediction prediction : predictions) {
            PredictionScoreInput predicted = new PredictionScoreInput(
                    prediction.getPredictedHomeScore(),
                    prediction.getPredictedAwayScore()
            );
            ScoreBreakdown breakdown = predictionScoringEngine.score(predicted, actual, rule);

            insertedEvents += scoreEventRepository.insertIgnoreConflict(
                    UUID.randomUUID(),
                    now,
                    now,
                    prediction.getPool().getId(),
                    prediction.getUser() == null ? null : prediction.getUser().getId(),
                    prediction.getManagedParticipant() == null ? null : prediction.getManagedParticipant().getId(),
                    prediction.getMatch().getId(),
                    prediction.getId(),
                    breakdown.totalPoints(),
                    breakdown.exactScorePointsAwarded(),
                    breakdown.outcomePointsAwarded(),
                    breakdown.goalDifferenceBonusPointsAwarded(),
                    breakdown.explanation(),
                    rule.version(),
                    checksum,
                    now
            );

            predictionCurrentScoreRepository.findByPredictionId(prediction.getId())
                    .map(existing -> {
                        existing.updateScore(breakdown.totalPoints(), rule.version(), checksum, now);
                        return predictionCurrentScoreRepository.save(existing);
                    })
                    .orElseGet(() -> predictionCurrentScoreRepository.save(createCurrentScore(prediction, breakdown, rule, checksum, now)));
        }

        Set<UUID> affectedPools = predictions.stream()
                .map(prediction -> prediction.getPool().getId())
                .collect(Collectors.toSet());

        if (!affectedPools.isEmpty()) {
            poolLeaderboardRecalculationService.rebuild(new ArrayList<>(affectedPools), now);
        }

        return new RecalculationResult(
                command.matchId(),
                checksum,
                insertedEvents,
                affectedPools.size(),
                insertedEvents == 0
        );
    }

    private PredictionCurrentScore createCurrentScore(Prediction prediction,
                                                      ScoreBreakdown breakdown,
                                                      ScoringRuleDefinition rule,
                                                      String checksum,
                                                      Instant now) {
        if (prediction.isManagedParticipantPrediction()) {
            return new PredictionCurrentScore(
                    prediction,
                    prediction.getPool(),
                    prediction.getManagedParticipant(),
                    prediction.getMatch(),
                    breakdown.totalPoints(),
                    rule.version(),
                    checksum,
                    now
            );
        }
        return new PredictionCurrentScore(
                prediction,
                prediction.getPool(),
                prediction.getUser(),
                prediction.getMatch(),
                breakdown.totalPoints(),
                rule.version(),
                checksum,
                now
        );
    }

    private String checksumFor(UpsertMatchResultCommand command) {
        String raw = command.matchId() + "|" + command.homeScore() + "|" + command.awayScore()
                + "|" + command.homePenaltyScore() + "|" + command.awayPenaltyScore() + "|" + command.finalResult();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 should be available", e);
        }
    }
}
