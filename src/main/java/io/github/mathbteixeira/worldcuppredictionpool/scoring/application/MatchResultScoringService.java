package io.github.mathbteixeira.worldcuppredictionpool.scoring.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.ManagedParticipant;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.ManagedParticipantRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence.PredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.domain.LeaderboardEntry;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.domain.PredictionCurrentScore;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.MatchScoreInput;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.PredictionScoreInput;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.PredictionScoringEngine;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoreBreakdown;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoringRuleDefinition;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.LeaderboardEntryRepository;
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
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final PoolMembershipRepository poolMembershipRepository;
    private final ManagedParticipantRepository managedParticipantRepository;
    private final PredictionScoringEngine predictionScoringEngine;
    private final ScoringRuleResolver scoringRuleResolver;
    private final Clock clock;

    public MatchResultScoringService(MatchRepository matchRepository,
                                     MatchResultRepository matchResultRepository,
                                     PredictionRepository predictionRepository,
                                     ScoreEventRepository scoreEventRepository,
                                     PredictionCurrentScoreRepository predictionCurrentScoreRepository,
                                     LeaderboardEntryRepository leaderboardEntryRepository,
                                     PoolMembershipRepository poolMembershipRepository,
                                     ManagedParticipantRepository managedParticipantRepository,
                                     PredictionScoringEngine predictionScoringEngine,
                                     ScoringRuleResolver scoringRuleResolver,
                                     Clock clock) {
        this.matchRepository = matchRepository;
        this.matchResultRepository = matchResultRepository;
        this.predictionRepository = predictionRepository;
        this.scoreEventRepository = scoreEventRepository;
        this.predictionCurrentScoreRepository = predictionCurrentScoreRepository;
        this.leaderboardEntryRepository = leaderboardEntryRepository;
        this.poolMembershipRepository = poolMembershipRepository;
        this.managedParticipantRepository = managedParticipantRepository;
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
            rebuildLeaderboard(new ArrayList<>(affectedPools), now);
        }

        return new RecalculationResult(
                command.matchId(),
                checksum,
                insertedEvents,
                affectedPools.size(),
                insertedEvents == 0
        );
    }

    private void rebuildLeaderboard(List<UUID> poolIds, Instant now) {
        leaderboardEntryRepository.deleteByPoolIdIn(poolIds);

        List<PoolMembership> memberships = poolMembershipRepository.findAllByPoolIdIn(poolIds);
        List<ManagedParticipant> managedParticipants = managedParticipantRepository.findAllByPoolIdIn(poolIds);
        Map<UUID, PredictionPool> poolsById = memberships.stream()
                .collect(Collectors.toMap(membership -> membership.getPool().getId(), PoolMembership::getPool, (first, ignored) -> first));
        managedParticipants.forEach(participant -> poolsById.putIfAbsent(participant.getPool().getId(), participant.getPool()));
        Map<UUID, PoolMembership> membershipsByUserId = memberships.stream()
                .collect(Collectors.toMap(membership -> membership.getUser().getId(), membership -> membership, (first, ignored) -> first));
        Map<UUID, ManagedParticipant> managedParticipantsById = managedParticipants.stream()
                .collect(Collectors.toMap(ManagedParticipant::getId, participant -> participant));

        Map<UUID, Map<ParticipantKey, Integer>> totalsByPoolAndParticipant = new LinkedHashMap<>();
        for (PoolMembership membership : memberships) {
            totalsByPoolAndParticipant
                    .computeIfAbsent(membership.getPool().getId(), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(ParticipantKey.user(membership.getUser().getId()), 0);
        }
        for (ManagedParticipant participant : managedParticipants) {
            totalsByPoolAndParticipant
                    .computeIfAbsent(participant.getPool().getId(), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(ParticipantKey.managed(participant.getId()), 0);
        }

        for (PredictionCurrentScoreRepository.PoolUserTotalProjection aggregate : predictionCurrentScoreRepository.aggregateTotalsByPoolAndUser(poolIds)) {
            totalsByPoolAndParticipant
                    .computeIfAbsent(aggregate.getPoolId(), ignored -> new LinkedHashMap<>())
                    .put(ParticipantKey.user(aggregate.getUserId()), Math.toIntExact(Objects.requireNonNullElse(aggregate.getTotalPoints(), 0L)));
        }
        for (PredictionCurrentScoreRepository.PoolManagedParticipantTotalProjection aggregate : predictionCurrentScoreRepository.aggregateTotalsByPoolAndManagedParticipant(poolIds)) {
            totalsByPoolAndParticipant
                    .computeIfAbsent(aggregate.getPoolId(), ignored -> new LinkedHashMap<>())
                    .put(ParticipantKey.managed(aggregate.getManagedParticipantId()), Math.toIntExact(Objects.requireNonNullElse(aggregate.getTotalPoints(), 0L)));
        }

        List<LeaderboardEntry> rebuilt = new ArrayList<>();
        for (Map.Entry<UUID, Map<ParticipantKey, Integer>> poolEntry : totalsByPoolAndParticipant.entrySet()) {
            List<Map.Entry<ParticipantKey, Integer>> ranking = poolEntry.getValue().entrySet().stream()
                    .sorted(Comparator
                            .comparing(Map.Entry<ParticipantKey, Integer>::getValue, Comparator.reverseOrder())
                            .thenComparing(entry -> entry.getKey().sortValue()))
                    .toList();

            int rank = 1;
            Integer previousTotal = null;
            for (int index = 0; index < ranking.size(); index++) {
                Map.Entry<ParticipantKey, Integer> participantTotal = ranking.get(index);
                if (previousTotal != null && !previousTotal.equals(participantTotal.getValue())) {
                    rank = index + 1;
                }
                previousTotal = participantTotal.getValue();

                ParticipantKey participantKey = participantTotal.getKey();
                PredictionPool poolReference = poolsById.get(poolEntry.getKey());
                if (participantKey.managed()) {
                    rebuilt.add(new LeaderboardEntry(
                            poolReference,
                            managedParticipantsById.get(participantKey.id()),
                            participantTotal.getValue(),
                            rank,
                            now
                    ));
                } else {
                    rebuilt.add(new LeaderboardEntry(
                            poolReference,
                            membershipsByUserId.get(participantKey.id()).getUser(),
                            participantTotal.getValue(),
                            rank,
                            now
                    ));
                }
            }
        }

        if (!rebuilt.isEmpty()) {
            leaderboardEntryRepository.saveAll(rebuilt);
        }
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

    private record ParticipantKey(UUID id, boolean managed) {
        static ParticipantKey user(UUID id) {
            return new ParticipantKey(id, false);
        }

        static ParticipantKey managed(UUID id) {
            return new ParticipantKey(id, true);
        }

        String sortValue() {
            return (managed ? "managed:" : "user:") + id;
        }
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
