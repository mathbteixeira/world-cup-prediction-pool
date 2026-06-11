package io.github.mathbteixeira.worldcuppredictionpool.groupstage.application;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Derives the group composition of a tournament from its group-stage matches.
 * Groups are not stored explicitly; a group's teams are exactly the resolved
 * teams appearing in that group's {@code GROUP_STAGE} matches, and the group's
 * prediction deadline is the earliest kickoff among those matches.
 */
@Component
public class TournamentGroupCatalog {

    private static final String GROUP_STAGE = "GROUP_STAGE";

    private final MatchRepository matchRepository;

    public TournamentGroupCatalog(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    /** Ordered map of group name to group info (both keyed/sorted alphabetically). */
    public Map<String, GroupInfo> groupsOf(UUID tournamentId) {
        record MutableGroup(Map<UUID, Team> teams, Instant[] earliest) {
        }
        Map<String, MutableGroup> accumulator = new LinkedHashMap<>();

        for (Match match : matchRepository.findAllByTournamentIdOrderByKickoffAtAsc(tournamentId)) {
            if (!GROUP_STAGE.equals(match.getStage()) || match.getGroupName() == null) {
                continue;
            }
            MutableGroup group = accumulator.computeIfAbsent(
                    match.getGroupName(),
                    ignored -> new MutableGroup(new LinkedHashMap<>(), new Instant[1]));
            registerTeam(group.teams(), match.getHomeTeam());
            registerTeam(group.teams(), match.getAwayTeam());
            if (group.earliest()[0] == null || match.getKickoffAt().isBefore(group.earliest()[0])) {
                group.earliest()[0] = match.getKickoffAt();
            }
        }

        Map<String, GroupInfo> groups = new LinkedHashMap<>();
        accumulator.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    List<Team> teams = entry.getValue().teams().values().stream()
                            .sorted(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER))
                            .toList();
                    groups.put(entry.getKey(), new GroupInfo(entry.getKey(), teams, entry.getValue().earliest()[0]));
                });
        return groups;
    }

    public Optional<GroupInfo> group(UUID tournamentId, String groupName) {
        return Optional.ofNullable(groupsOf(tournamentId).get(groupName));
    }

    private void registerTeam(Map<UUID, Team> teams, Team team) {
        if (team != null) {
            teams.putIfAbsent(team.getId(), team);
        }
    }

    /**
     * A single group's composition.
     *
     * @param groupName       the single-letter group name
     * @param teams           the group's teams, sorted by name
     * @param earliestKickoff the earliest kickoff among the group's matches, used
     *                        as the prediction deadline (nullable if unknown)
     */
    public record GroupInfo(String groupName, List<Team> teams, Instant earliestKickoff) {

        public boolean predictionsOpenAt(Instant now) {
            return earliestKickoff == null || now.isBefore(earliestKickoff);
        }
    }
}