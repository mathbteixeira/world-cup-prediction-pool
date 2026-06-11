package io.github.mathbteixeira.worldcuppredictionpool.groupstage.application;

import io.github.mathbteixeira.worldcuppredictionpool.groupstage.api.GroupStandingResponse;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.api.GroupTeamResponse;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.domain.GroupOfficialStanding;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.domain.GroupStandingPrediction;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence.GroupOfficialStandingRepository;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence.GroupStandingPredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class GroupStandingPredictionService {

    private final PredictionPoolRepository predictionPoolRepository;
    private final PoolMembershipRepository poolMembershipRepository;
    private final UserAccountRepository userAccountRepository;
    private final GroupStandingPredictionRepository groupStandingPredictionRepository;
    private final GroupOfficialStandingRepository groupOfficialStandingRepository;
    private final TournamentGroupCatalog tournamentGroupCatalog;
    private final Clock clock;

    public GroupStandingPredictionService(PredictionPoolRepository predictionPoolRepository,
                                          PoolMembershipRepository poolMembershipRepository,
                                          UserAccountRepository userAccountRepository,
                                          GroupStandingPredictionRepository groupStandingPredictionRepository,
                                          GroupOfficialStandingRepository groupOfficialStandingRepository,
                                          TournamentGroupCatalog tournamentGroupCatalog,
                                          Clock clock) {
        this.predictionPoolRepository = predictionPoolRepository;
        this.poolMembershipRepository = poolMembershipRepository;
        this.userAccountRepository = userAccountRepository;
        this.groupStandingPredictionRepository = groupStandingPredictionRepository;
        this.groupOfficialStandingRepository = groupOfficialStandingRepository;
        this.tournamentGroupCatalog = tournamentGroupCatalog;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<GroupStandingResponse> listGroups(UUID poolId, String userEmail) {
        PredictionPool pool = requireTournamentPool(poolId);
        UserAccount user = requireMember(poolId, userEmail);

        UUID tournamentId = pool.getTournament().getId();
        Instant now = Instant.now(clock);

        Map<String, GroupStandingPrediction> predictionsByGroup = new LinkedHashMap<>();
        for (GroupStandingPrediction prediction
                : groupStandingPredictionRepository.findAllByPoolIdAndUserIdOrderByGroupNameAsc(poolId, user.getId())) {
            predictionsByGroup.put(prediction.getGroupName(), prediction);
        }
        Map<String, GroupOfficialStanding> standingsByGroup = new LinkedHashMap<>();
        for (GroupOfficialStanding standing : groupOfficialStandingRepository.findAllByTournamentId(tournamentId)) {
            standingsByGroup.put(standing.getGroupName(), standing);
        }

        return tournamentGroupCatalog.groupsOf(tournamentId).values().stream()
                .map(group -> toResponse(
                        poolId,
                        tournamentId,
                        group,
                        Optional.ofNullable(predictionsByGroup.get(group.groupName())),
                        Optional.ofNullable(standingsByGroup.get(group.groupName())),
                        now))
                .toList();
    }

    @Transactional
    public GroupStandingResponse submit(UUID poolId, String groupName, String userEmail, List<UUID> teamIdsByPosition) {
        PredictionPool pool = requireTournamentPool(poolId);
        UserAccount user = requireMember(poolId, userEmail);

        UUID tournamentId = pool.getTournament().getId();
        TournamentGroupCatalog.GroupInfo group = tournamentGroupCatalog.group(tournamentId, groupName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        Instant now = Instant.now(clock);
        if (!group.predictionsOpenAt(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group predictions are closed");
        }

        Map<UUID, Team> teamsById = validateOrdering(group, teamIdsByPosition);
        Team first = teamsById.get(teamIdsByPosition.get(0));
        Team second = teamsById.get(teamIdsByPosition.get(1));
        Team third = teamsById.get(teamIdsByPosition.get(2));
        Team fourth = teamsById.get(teamIdsByPosition.get(3));

        GroupStandingPrediction saved = groupStandingPredictionRepository
                .findByPoolIdAndUserIdAndGroupName(poolId, user.getId(), groupName)
                .map(existing -> {
                    existing.resubmit(first, second, third, fourth, now);
                    return groupStandingPredictionRepository.save(existing);
                })
                .orElseGet(() -> groupStandingPredictionRepository.save(new GroupStandingPrediction(
                        pool,
                        user,
                        pool.getTournament(),
                        groupName,
                        first,
                        second,
                        third,
                        fourth,
                        now
                )));

        Optional<GroupOfficialStanding> official = groupOfficialStandingRepository
                .findByTournamentIdAndGroupName(tournamentId, groupName);
        return toResponse(poolId, tournamentId, group, Optional.of(saved), official, now);
    }

    /**
     * Validates that the submitted ordering is a permutation of exactly the
     * group's teams and returns those teams indexed by id.
     */
    private Map<UUID, Team> validateOrdering(TournamentGroupCatalog.GroupInfo group, List<UUID> teamIdsByPosition) {
        Map<UUID, Team> teamsById = new LinkedHashMap<>();
        group.teams().forEach(team -> teamsById.put(team.getId(), team));

        if (teamIdsByPosition.size() != teamsById.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Group %s requires exactly %d ordered teams".formatted(group.groupName(), teamsById.size()));
        }
        Set<UUID> submitted = new HashSet<>(teamIdsByPosition);
        if (submitted.size() != teamIdsByPosition.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each team can be ranked only once");
        }
        if (!submitted.equals(teamsById.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Submitted teams must match the teams in group " + group.groupName());
        }
        return teamsById;
    }

    private GroupStandingResponse toResponse(UUID poolId,
                                             UUID tournamentId,
                                             TournamentGroupCatalog.GroupInfo group,
                                             Optional<GroupStandingPrediction> prediction,
                                             Optional<GroupOfficialStanding> official,
                                             Instant now) {
        List<GroupTeamResponse> teams = group.teams().stream()
                .map(team -> new GroupTeamResponse(team.getId(), team.getName(), team.getFifaCode()))
                .toList();
        return new GroupStandingResponse(
                poolId,
                tournamentId,
                group.groupName(),
                teams,
                group.earliestKickoff(),
                group.predictionsOpenAt(now),
                prediction.map(GroupStandingPrediction::orderedTeamIds).orElse(null),
                prediction.map(GroupStandingPrediction::getSubmittedAt).orElse(null),
                official.map(GroupOfficialStanding::isConfirmed).orElse(false),
                official.filter(GroupOfficialStanding::isConfirmed).map(GroupOfficialStanding::orderedTeamIds).orElse(null)
        );
    }

    private PredictionPool requireTournamentPool(UUID poolId) {
        PredictionPool pool = predictionPoolRepository.findById(poolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found"));
        if (pool.isSingleMatchPool()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Group predictions are available only for tournament pools");
        }
        return pool;
    }

    private UserAccount requireMember(UUID poolId, String userEmail) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (poolMembershipRepository.findByPoolIdAndUserId(poolId, user.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this pool");
        }
        return user;
    }
}