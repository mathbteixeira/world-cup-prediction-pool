package io.github.mathbteixeira.worldcuppredictionpool.topscorer.application;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.PoolLeaderboardRecalculationService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.ScoringRuleResolver;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.StandingsRecalculationResult;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoringRuleDefinition;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.TopScorerScoreBreakdown;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.TopScorerScoringEngine;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain.TopScorerCurrentScore;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain.TopScorerPrediction;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain.TournamentTopScorerResult;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence.TopScorerCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence.TopScorerPredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence.TopScorerScoreEventRepository;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence.TournamentTopScorerResultRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Player;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
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
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TopScorerScoringService {

    private final TournamentRepository tournamentRepository;
    private final TopScorerSupport support;
    private final TournamentTopScorerResultRepository tournamentTopScorerResultRepository;
    private final TopScorerPredictionRepository topScorerPredictionRepository;
    private final TopScorerScoreEventRepository topScorerScoreEventRepository;
    private final TopScorerCurrentScoreRepository topScorerCurrentScoreRepository;
    private final TopScorerScoringEngine topScorerScoringEngine;
    private final ScoringRuleResolver scoringRuleResolver;
    private final PoolLeaderboardRecalculationService poolLeaderboardRecalculationService;
    private final Clock clock;

    public TopScorerScoringService(TournamentRepository tournamentRepository,
                                   TopScorerSupport support,
                                   TournamentTopScorerResultRepository tournamentTopScorerResultRepository,
                                   TopScorerPredictionRepository topScorerPredictionRepository,
                                   TopScorerScoreEventRepository topScorerScoreEventRepository,
                                   TopScorerCurrentScoreRepository topScorerCurrentScoreRepository,
                                   TopScorerScoringEngine topScorerScoringEngine,
                                   ScoringRuleResolver scoringRuleResolver,
                                   PoolLeaderboardRecalculationService poolLeaderboardRecalculationService,
                                   Clock clock) {
        this.tournamentRepository = tournamentRepository;
        this.support = support;
        this.tournamentTopScorerResultRepository = tournamentTopScorerResultRepository;
        this.topScorerPredictionRepository = topScorerPredictionRepository;
        this.topScorerScoreEventRepository = topScorerScoreEventRepository;
        this.topScorerCurrentScoreRepository = topScorerCurrentScoreRepository;
        this.topScorerScoringEngine = topScorerScoringEngine;
        this.scoringRuleResolver = scoringRuleResolver;
        this.poolLeaderboardRecalculationService = poolLeaderboardRecalculationService;
        this.clock = clock;
    }

    @Transactional
    public StandingsRecalculationResult confirmAndRecalculate(UUID tournamentId, UUID playerId, int goals) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found"));
        Player player = support.resolvePlayer(tournamentId, playerId);
        Instant now = Instant.now(clock);
        String checksum = checksumFor(tournamentId, playerId, goals);

        tournamentTopScorerResultRepository.findByTournamentId(tournamentId)
                .map(existing -> {
                    existing.updateResult(player, goals, true, now, checksum);
                    return tournamentTopScorerResultRepository.save(existing);
                })
                .orElseGet(() -> tournamentTopScorerResultRepository.save(new TournamentTopScorerResult(
                        tournament, player, goals, true, now, checksum)));

        ScoringRuleDefinition rule = scoringRuleResolver.resolve(tournamentId);
        List<TopScorerPrediction> predictions = topScorerPredictionRepository.findAllByTournamentId(tournamentId);
        int insertedEvents = 0;
        for (TopScorerPrediction prediction : predictions) {
            TopScorerScoreBreakdown breakdown = topScorerScoringEngine.score(
                    prediction.getPlayer().getId(),
                    prediction.getPredictedGoals(),
                    playerId,
                    goals,
                    rule);

            insertedEvents += topScorerScoreEventRepository.insertIgnoreConflict(
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
                    breakdown.explanation(),
                    rule.version(),
                    checksum,
                    now);

            topScorerCurrentScoreRepository.findByPredictionId(prediction.getId())
                    .map(existing -> {
                        existing.updateScore(breakdown.totalPoints(), rule.version(), checksum, now);
                        return topScorerCurrentScoreRepository.save(existing);
                    })
                    .orElseGet(() -> topScorerCurrentScoreRepository.save(new TopScorerCurrentScore(
                            prediction,
                            prediction.getPool(),
                            prediction.getUser(),
                            breakdown.totalPoints(),
                            rule.version(),
                            checksum,
                            now)));
        }

        Set<UUID> affectedPools = predictions.stream()
                .map(prediction -> prediction.getPool().getId())
                .collect(Collectors.toCollection(HashSet::new));
        poolLeaderboardRecalculationService.rebuild(new ArrayList<>(affectedPools), now);

        return new StandingsRecalculationResult(checksum, predictions.size(), affectedPools.size(), insertedEvents == 0);
    }

    private String checksumFor(UUID tournamentId, UUID playerId, int goals) {
        String raw = tournamentId + "|" + playerId + "|" + goals;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 should be available", e);
        }
    }
}
