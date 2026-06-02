package io.github.mathbteixeira.worldcuppredictionpool.tournament.application;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.MatchResultResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.MatchSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TeamSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchResult;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchResultRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class MatchParticipantResolutionService {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final MatchResultRepository matchResultRepository;
    private final Clock clock;

    public MatchParticipantResolutionService(MatchRepository matchRepository,
                                             TeamRepository teamRepository,
                                             MatchResultRepository matchResultRepository,
                                             Clock clock) {
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.matchResultRepository = matchResultRepository;
        this.clock = clock;
    }

    @Transactional
    public MatchSummaryResponse resolve(ResolveMatchParticipantsCommand command) {
        if (command.homeTeamId().equals(command.awayTeamId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Home and away teams must be different");
        }

        Match match = matchRepository.findById(command.matchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));
        if (match.hasResolvedTeams()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match participants are already resolved");
        }

        Team homeTeam = teamRepository.findById(command.homeTeamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Home team not found"));
        Team awayTeam = teamRepository.findById(command.awayTeamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Away team not found"));

        if (!homeTeam.getTournament().getId().equals(match.getTournament().getId())
                || !awayTeam.getTournament().getId().equals(match.getTournament().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Teams must belong to the match tournament");
        }

        match.resolveParticipants(homeTeam, awayTeam);
        Match resolvedMatch = matchRepository.save(match);

        return toResponse(
                resolvedMatch,
                matchResultRepository.findByMatchId(command.matchId()),
                Instant.now(clock)
        );
    }

    private MatchSummaryResponse toResponse(Match match, Optional<MatchResult> result, Instant now) {
        return new MatchSummaryResponse(
                match.getId(),
                match.getTournament().getId(),
                toTeamResponse(match.getHomeTeam()),
                toTeamResponse(match.getAwayTeam()),
                match.getHomePlaceholder(),
                match.getAwayPlaceholder(),
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
