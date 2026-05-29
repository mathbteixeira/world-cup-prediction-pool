package io.github.mathbteixeira.worldcuppredictionpool.tournament.application;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.MatchResultResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.MatchSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TeamSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchResult;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchResultRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TournamentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TournamentMatchService {

    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;
    private final Clock clock;

    public TournamentMatchService(TournamentRepository tournamentRepository,
                                  MatchRepository matchRepository,
                                  MatchResultRepository matchResultRepository,
                                  Clock clock) {
        this.tournamentRepository = tournamentRepository;
        this.matchRepository = matchRepository;
        this.matchResultRepository = matchResultRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MatchSummaryResponse> listMatches(UUID tournamentId,
                                                  MatchStatus status,
                                                  String stage,
                                                  String groupName,
                                                  Instant from,
                                                  Instant to,
                                                  boolean predictableOnly) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found");
        }

        Instant now = Instant.now(clock);
        List<Match> matches = matchRepository.findAllByTournamentIdOrderByKickoffAtAsc(tournamentId).stream()
                .filter(match -> status == null || match.getStatus() == status)
                .filter(match -> matchesIgnoreCase(match.getStage(), stage))
                .filter(match -> matchesIgnoreCase(match.getGroupName(), groupName))
                .filter(match -> from == null || !match.getKickoffAt().isBefore(from))
                .filter(match -> to == null || !match.getKickoffAt().isAfter(to))
                .filter(match -> !predictableOnly || match.canAcceptPredictionsAt(now))
                .toList();

        if (matches.isEmpty()) {
            return List.of();
        }

        Map<UUID, MatchResult> resultsByMatchId = matchResultRepository.findAllByMatchIdIn(
                        matches.stream().map(Match::getId).toList())
                .stream()
                .collect(Collectors.toMap(result -> result.getMatch().getId(), Function.identity()));

        return matches.stream()
                .map(match -> toResponse(match, Optional.ofNullable(resultsByMatchId.get(match.getId())), now))
                .toList();
    }

    private boolean matchesIgnoreCase(String value, String filter) {
        return filter == null || filter.isBlank() || (value != null && value.equalsIgnoreCase(filter.trim()));
    }

    private MatchSummaryResponse toResponse(Match match, Optional<MatchResult> result, Instant now) {
        return new MatchSummaryResponse(
                match.getId(),
                match.getTournament().getId(),
                toTeamResponse(match.getHomeTeam()),
                toTeamResponse(match.getAwayTeam()),
                match.getKickoffAt(),
                match.getStage(),
                match.getGroupName(),
                match.getStatus(),
                result.map(this::toResultResponse).orElse(null),
                match.canAcceptPredictionsAt(now)
        );
    }

    private TeamSummaryResponse toTeamResponse(Team team) {
        return new TeamSummaryResponse(team.getId(), team.getName(), team.getFifaCode());
    }

    private MatchResultResponse toResultResponse(MatchResult result) {
        return new MatchResultResponse(
                result.getHomeScore(),
                result.getAwayScore(),
                result.getHomePenaltyScore(),
                result.getAwayPenaltyScore(),
                result.isFinalResult()
        );
    }
}
