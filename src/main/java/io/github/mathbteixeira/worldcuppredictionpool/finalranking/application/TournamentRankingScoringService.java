package io.github.mathbteixeira.worldcuppredictionpool.finalranking.application;

import io.github.mathbteixeira.worldcuppredictionpool.finalranking.domain.TournamentOfficialRanking;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.domain.TournamentRankingCurrentScore;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.domain.TournamentRankingPrediction;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence.TournamentOfficialRankingRepository;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence.TournamentRankingCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence.TournamentRankingPredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence.TournamentRankingScoreEventRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.PoolLeaderboardRecalculationService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.ScoringRuleResolver;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.StandingsRecalculationResult;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoringRuleDefinition;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.TournamentRankingScoreBreakdown;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.TournamentRankingScoringEngine;
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

/**
 * Confirms the official tournament final ranking and recalculates all podium
 * predictions and affected pool leaderboards in one transaction. Re-confirming
 * the same ranking is idempotent; confirming a corrected ranking rescores.
 */
@Service
public class TournamentRankingScoringService {

    private final TournamentRepository tournamentRepository;
    private final TournamentRankingSupport support;
    private final TournamentOfficialRankingRepository tournamentOfficialRankingRepository;
    private final TournamentRankingPredictionRepository tournamentRankingPredictionRepository;
    private final TournamentRankingScoreEventRepository tournamentRankingScoreEventRepository;
    private final TournamentRankingCurrentScoreRepository tournamentRankingCurrentScoreRepository;
    private final TournamentRankingScoringEngine tournamentRankingScoringEngine;
    private final ScoringRuleResolver scoringRuleResolver;
    private final PoolLeaderboardRecalculationService poolLeaderboardRecalculationService;
    private final Clock clock;

    public TournamentRankingScoringService(TournamentRepository tournamentRepository,
                                           TournamentRankingSupport support,
                                           TournamentOfficialRankingRepository tournamentOfficialRankingRepository,
                                           TournamentRankingPredictionRepository tournamentRankingPredictionRepository,
                                           TournamentRankingScoreEventRepository tournamentRankingScoreEventRepository,
                                           TournamentRankingCurrentScoreRepository tournamentRankingCurrentScoreRepository,
                                           TournamentRankingScoringEngine tournamentRankingScoringEngine,
                                           ScoringRuleResolver scoringRuleResolver,
                                           PoolLeaderboardRecalculationService poolLeaderboardRecalculationService,
                                           Clock clock) {
        this.tournamentRepository = tournamentRepository;
        this.support = support;
        this.tournamentOfficialRankingRepository = tournamentOfficialRankingRepository;
        this.tournamentRankingPredictionRepository = tournamentRankingPredictionRepository;
        this.tournamentRankingScoreEventRepository = tournamentRankingScoreEventRepository;
        this.tournamentRankingCurrentScoreRepository = tournamentRankingCurrentScoreRepository;
        this.tournamentRankingScoringEngine = tournamentRankingScoringEngine;
        this.scoringRuleResolver = scoringRuleResolver;
        this.poolLeaderboardRecalculationService = poolLeaderboardRecalculationService;
        this.clock = clock;
    }

    @Transactional
    public StandingsRecalculationResult confirmAndRecalculate(UUID tournamentId,
                                                              UUID championId,
                                                              UUID runnerUpId,
                                                              UUID thirdId,
                                                              UUID fourthId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found"));

        TournamentRankingSupport.ResolvedPodium podium =
                support.resolvePodium(tournamentId, championId, runnerUpId, thirdId, fourthId);

        Instant now = Instant.now(clock);
        List<UUID> actualOrder = List.of(championId, runnerUpId, thirdId, fourthId);
        String checksum = checksumFor(tournamentId, actualOrder);

        tournamentOfficialRankingRepository.findByTournamentId(tournamentId)
                .map(existing -> {
                    existing.updateRanking(podium.champion(), podium.runnerUp(), podium.third(), podium.fourth(), true, now, checksum);
                    return tournamentOfficialRankingRepository.save(existing);
                })
                .orElseGet(() -> tournamentOfficialRankingRepository.save(new TournamentOfficialRanking(
                        tournament, podium.champion(), podium.runnerUp(), podium.third(), podium.fourth(), true, now, checksum)));

        ScoringRuleDefinition rule = scoringRuleResolver.resolve(tournamentId);
        List<TournamentRankingPrediction> predictions =
                tournamentRankingPredictionRepository.findAllByTournamentId(tournamentId);

        int insertedEvents = 0;
        for (TournamentRankingPrediction prediction : predictions) {
            TournamentRankingScoreBreakdown breakdown =
                    tournamentRankingScoringEngine.score(prediction.orderedTeamIds(), actualOrder, rule);

            insertedEvents += tournamentRankingScoreEventRepository.insertIgnoreConflict(
                    UUID.randomUUID(),
                    now,
                    now,
                    prediction.getPool().getId(),
                    prediction.getUser().getId(),
                    tournamentId,
                    prediction.getId(),
                    breakdown.totalPoints(),
                    breakdown.championPointsAwarded(),
                    breakdown.runnerUpPointsAwarded(),
                    breakdown.thirdPlacePointsAwarded(),
                    breakdown.fourthPlacePointsAwarded(),
                    breakdown.explanation(),
                    rule.version(),
                    checksum,
                    now
            );

            tournamentRankingCurrentScoreRepository.findByPredictionId(prediction.getId())
                    .map(existing -> {
                        existing.updateScore(breakdown.totalPoints(), rule.version(), checksum, now);
                        return tournamentRankingCurrentScoreRepository.save(existing);
                    })
                    .orElseGet(() -> tournamentRankingCurrentScoreRepository.save(new TournamentRankingCurrentScore(
                            prediction,
                            prediction.getPool(),
                            prediction.getUser(),
                            breakdown.totalPoints(),
                            rule.version(),
                            checksum,
                            now
                    )));
        }

        Set<UUID> affectedPools = predictions.stream()
                .map(prediction -> prediction.getPool().getId())
                .collect(Collectors.toCollection(HashSet::new));
        poolLeaderboardRecalculationService.rebuild(new ArrayList<>(affectedPools), now);

        return new StandingsRecalculationResult(
                checksum,
                predictions.size(),
                affectedPools.size(),
                insertedEvents == 0
        );
    }

    private String checksumFor(UUID tournamentId, List<UUID> orderedTeamIds) {
        String raw = tournamentId + "|"
                + orderedTeamIds.stream().map(UUID::toString).collect(Collectors.joining("|"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 should be available", e);
        }
    }
}