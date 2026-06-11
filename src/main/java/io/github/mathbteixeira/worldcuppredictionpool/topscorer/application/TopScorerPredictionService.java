package io.github.mathbteixeira.worldcuppredictionpool.topscorer.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.api.TopScorerPick;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.api.TopScorerResponse;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain.TopScorerPrediction;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence.TopScorerPredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TeamSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TeamRepository;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TopScorerPredictionService {

    private final TopScorerSupport support;
    private final TeamRepository teamRepository;
    private final TopScorerPredictionRepository topScorerPredictionRepository;
    private final Clock clock;

    public TopScorerPredictionService(TopScorerSupport support,
                                      TeamRepository teamRepository,
                                      TopScorerPredictionRepository topScorerPredictionRepository,
                                      Clock clock) {
        this.support = support;
        this.teamRepository = teamRepository;
        this.topScorerPredictionRepository = topScorerPredictionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TopScorerResponse getTopScorer(UUID poolId, String userEmail) {
        PredictionPool pool = support.requireTournamentPool(poolId);
        UserAccount user = support.requireMember(poolId, userEmail);
        UUID tournamentId = pool.getTournament().getId();

        Optional<TopScorerPrediction> prediction = topScorerPredictionRepository.findByPoolIdAndUserId(poolId, user.getId());
        return toResponse(poolId, tournamentId, prediction);
    }

    @Transactional
    public TopScorerResponse submit(UUID poolId, String userEmail, UUID teamId, String playerName, int goals) {
        if (!support.predictionOpen()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Top-scorer predictions are closed");
        }
        PredictionPool pool = support.requireTournamentPool(poolId);
        UserAccount user = support.requireMember(poolId, userEmail);
        UUID tournamentId = pool.getTournament().getId();
        Team team = support.resolveTeam(tournamentId, teamId);
        String normalizedPlayerName = normalizePlayerName(playerName);
        Instant now = Instant.now(clock);

        TopScorerPrediction saved = topScorerPredictionRepository.findByPoolIdAndUserId(poolId, user.getId())
                .map(existing -> {
                    existing.resubmit(team, normalizedPlayerName, goals, now);
                    return topScorerPredictionRepository.save(existing);
                })
                .orElseGet(() -> topScorerPredictionRepository.save(new TopScorerPrediction(
                        pool, user, pool.getTournament(), team, normalizedPlayerName, goals, now
                )));

        return toResponse(poolId, tournamentId, Optional.of(saved));
    }

    private TopScorerResponse toResponse(UUID poolId,
                                         UUID tournamentId,
                                         Optional<TopScorerPrediction> prediction) {
        List<TeamSummaryResponse> teams = teamRepository.findAllByTournamentIdOrderByNameAsc(tournamentId).stream()
                .map(team -> new TeamSummaryResponse(team.getId(), team.getName(), team.getFifaCode()))
                .toList();
        return new TopScorerResponse(
                poolId,
                tournamentId,
                teams,
                support.predictionDeadline(),
                support.predictionOpen(),
                prediction.map(this::toPick).orElse(null),
                prediction.map(TopScorerPrediction::getSubmittedAt).orElse(null)
        );
    }

    private TopScorerPick toPick(TopScorerPrediction prediction) {
        return new TopScorerPick(prediction.getTeam().getId(), prediction.getPlayerName(), prediction.getPredictedGoals());
    }

    private String normalizePlayerName(String playerName) {
        String normalized = playerName == null ? "" : playerName.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player name is required");
        }
        return normalized;
    }
}
