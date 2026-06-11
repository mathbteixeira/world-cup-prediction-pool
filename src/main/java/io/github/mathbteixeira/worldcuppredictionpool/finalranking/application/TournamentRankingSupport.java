package io.github.mathbteixeira.worldcuppredictionpool.finalranking.application;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Shared helpers for the final-ranking feature: tournament team lookup, the
 * prediction deadline, and podium validation.
 * Centralised here so the prediction and scoring services stay consistent.
 */
@Component
public class TournamentRankingSupport {

    static final Instant PREDICTION_DEADLINE = Instant.parse("2026-06-16T03:00:00Z");

    private final TeamRepository teamRepository;

    public TournamentRankingSupport(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public List<Team> teamsOf(UUID tournamentId) {
        return teamRepository.findAllByTournamentIdOrderByNameAsc(tournamentId);
    }

    /** The fixed podium prediction deadline. */
    public Instant predictionDeadline(UUID tournamentId) {
        return PREDICTION_DEADLINE;
    }

    /**
     * Validates that the four podium picks are distinct teams of the tournament
     * and returns the managed {@link Team} entities in podium order.
     */
    public ResolvedPodium resolvePodium(UUID tournamentId, UUID championId, UUID runnerUpId, UUID thirdId, UUID fourthId) {
        List<UUID> ordered = List.of(championId, runnerUpId, thirdId, fourthId);
        Set<UUID> distinct = new LinkedHashSet<>(ordered);
        if (distinct.size() != ordered.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Podium teams must be four distinct teams");
        }

        Map<UUID, Team> teamsById = teamRepository.findAllByTournamentIdOrderByNameAsc(tournamentId).stream()
                .collect(Collectors.toMap(Team::getId, team -> team));
        for (UUID teamId : ordered) {
            if (!teamsById.containsKey(teamId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Team %s does not belong to the tournament".formatted(teamId));
            }
        }
        return new ResolvedPodium(
                teamsById.get(championId),
                teamsById.get(runnerUpId),
                teamsById.get(thirdId),
                teamsById.get(fourthId)
        );
    }

    public record ResolvedPodium(Team champion, Team runnerUp, Team third, Team fourth) {
    }
}
