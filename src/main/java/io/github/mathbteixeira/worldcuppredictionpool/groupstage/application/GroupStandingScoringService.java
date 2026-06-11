package io.github.mathbteixeira.worldcuppredictionpool.groupstage.application;

import io.github.mathbteixeira.worldcuppredictionpool.groupstage.domain.GroupOfficialStanding;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.domain.GroupStandingCurrentScore;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.domain.GroupStandingPrediction;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence.GroupOfficialStandingRepository;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence.GroupStandingCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence.GroupStandingPredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence.GroupStandingScoreEventRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.PoolLeaderboardRecalculationService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.ScoringRuleResolver;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.StandingsRecalculationResult;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.GroupStandingScoreBreakdown;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.GroupStandingScoringEngine;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoringRuleDefinition;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Confirms a group's official final standings and recalculates all group-position
 * predictions and affected pool leaderboards in one transaction. Re-confirming
 * the same standings is idempotent; confirming a corrected ordering rescores.
 */
@Service
public class GroupStandingScoringService {

    private final TournamentRepository tournamentRepository;
    private final TournamentGroupCatalog tournamentGroupCatalog;
    private final GroupOfficialStandingRepository groupOfficialStandingRepository;
    private final GroupStandingPredictionRepository groupStandingPredictionRepository;
    private final GroupStandingScoreEventRepository groupStandingScoreEventRepository;
    private final GroupStandingCurrentScoreRepository groupStandingCurrentScoreRepository;
    private final GroupStandingScoringEngine groupStandingScoringEngine;
    private final ScoringRuleResolver scoringRuleResolver;
    private final PoolLeaderboardRecalculationService poolLeaderboardRecalculationService;
    private final Clock clock;

    public GroupStandingScoringService(TournamentRepository tournamentRepository,
                                       TournamentGroupCatalog tournamentGroupCatalog,
                                       GroupOfficialStandingRepository groupOfficialStandingRepository,
                                       GroupStandingPredictionRepository groupStandingPredictionRepository,
                                       GroupStandingScoreEventRepository groupStandingScoreEventRepository,
                                       GroupStandingCurrentScoreRepository groupStandingCurrentScoreRepository,
                                       GroupStandingScoringEngine groupStandingScoringEngine,
                                       ScoringRuleResolver scoringRuleResolver,
                                       PoolLeaderboardRecalculationService poolLeaderboardRecalculationService,
                                       Clock clock) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentGroupCatalog = tournamentGroupCatalog;
        this.groupOfficialStandingRepository = groupOfficialStandingRepository;
        this.groupStandingPredictionRepository = groupStandingPredictionRepository;
        this.groupStandingScoreEventRepository = groupStandingScoreEventRepository;
        this.groupStandingCurrentScoreRepository = groupStandingCurrentScoreRepository;
        this.groupStandingScoringEngine = groupStandingScoringEngine;
        this.scoringRuleResolver = scoringRuleResolver;
        this.poolLeaderboardRecalculationService = poolLeaderboardRecalculationService;
        this.clock = clock;
    }

    @Transactional
    public StandingsRecalculationResult confirmAndRecalculate(UUID tournamentId, String groupName, List<UUID> teamIdsByPosition) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found"));
        TournamentGroupCatalog.GroupInfo group = tournamentGroupCatalog.group(tournamentId, groupName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        Map<UUID, Team> teamsById = validateOrdering(group, teamIdsByPosition);
        Team first = teamsById.get(teamIdsByPosition.get(0));
        Team second = teamsById.get(teamIdsByPosition.get(1));
        Team third = teamsById.get(teamIdsByPosition.get(2));
        Team fourth = teamsById.get(teamIdsByPosition.get(3));

        Instant now = Instant.now(clock);
        String checksum = checksumFor(tournamentId, groupName, teamIdsByPosition);

        groupOfficialStandingRepository.findByTournamentIdAndGroupName(tournamentId, groupName)
                .map(existing -> {
                    existing.updateStanding(first, second, third, fourth, true, now, checksum);
                    return groupOfficialStandingRepository.save(existing);
                })
                .orElseGet(() -> groupOfficialStandingRepository.save(new GroupOfficialStanding(
                        tournament, groupName, first, second, third, fourth, true, now, checksum)));

        ScoringRuleDefinition rule = scoringRuleResolver.resolve(tournamentId);
        List<UUID> actualOrder = teamIdsByPosition;
        List<GroupStandingPrediction> predictions =
                groupStandingPredictionRepository.findAllByTournamentIdAndGroupName(tournamentId, groupName);

        int insertedEvents = 0;
        for (GroupStandingPrediction prediction : predictions) {
            GroupStandingScoreBreakdown breakdown =
                    groupStandingScoringEngine.score(prediction.orderedTeamIds(), actualOrder, rule);

            insertedEvents += groupStandingScoreEventRepository.insertIgnoreConflict(
                    UUID.randomUUID(),
                    now,
                    now,
                    prediction.getPool().getId(),
                    prediction.getUser().getId(),
                    tournamentId,
                    prediction.getId(),
                    groupName,
                    breakdown.totalPoints(),
                    breakdown.correctPositions(),
                    breakdown.explanation(),
                    rule.version(),
                    checksum,
                    now
            );

            groupStandingCurrentScoreRepository.findByPredictionId(prediction.getId())
                    .map(existing -> {
                        existing.updateScore(breakdown.totalPoints(), rule.version(), checksum, now);
                        return groupStandingCurrentScoreRepository.save(existing);
                    })
                    .orElseGet(() -> groupStandingCurrentScoreRepository.save(new GroupStandingCurrentScore(
                            prediction,
                            prediction.getPool(),
                            prediction.getUser(),
                            groupName,
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

    private Map<UUID, Team> validateOrdering(TournamentGroupCatalog.GroupInfo group, List<UUID> teamIdsByPosition) {
        Map<UUID, Team> teamsById = new LinkedHashMap<>();
        group.teams().forEach(team -> teamsById.put(team.getId(), team));

        if (teamIdsByPosition.size() != teamsById.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Group %s requires exactly %d ordered teams".formatted(group.groupName(), teamsById.size()));
        }
        Set<UUID> submitted = new HashSet<>(teamIdsByPosition);
        if (submitted.size() != teamIdsByPosition.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each team can appear only once");
        }
        if (!submitted.equals(teamsById.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Submitted teams must match the teams in group " + group.groupName());
        }
        return teamsById;
    }

    private String checksumFor(UUID tournamentId, String groupName, List<UUID> orderedTeamIds) {
        String raw = tournamentId + "|" + groupName + "|"
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