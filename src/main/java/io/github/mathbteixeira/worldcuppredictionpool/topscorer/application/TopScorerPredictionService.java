package io.github.mathbteixeira.worldcuppredictionpool.topscorer.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.api.TopScorerPick;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.api.TopScorerResponse;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain.TopScorerPrediction;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain.TournamentTopScorerResult;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence.TopScorerPredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence.TournamentTopScorerResultRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TeamSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Player;
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
    private final TournamentTopScorerResultRepository tournamentTopScorerResultRepository;
    private final Clock clock;

    public TopScorerPredictionService(TopScorerSupport support,
                                      TeamRepository teamRepository,
                                      TopScorerPredictionRepository topScorerPredictionRepository,
                                      TournamentTopScorerResultRepository tournamentTopScorerResultRepository,
                                      Clock clock) {
        this.support = support;
        this.teamRepository = teamRepository;
        this.topScorerPredictionRepository = topScorerPredictionRepository;
        this.tournamentTopScorerResultRepository = tournamentTopScorerResultRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TopScorerResponse getTopScorer(UUID poolId, String userEmail) {
        PredictionPool pool = support.requireTournamentPool(poolId);
        UserAccount user = support.requireMember(poolId, userEmail);
        UUID tournamentId = pool.getTournament().getId();

        Optional<TopScorerPrediction> prediction = topScorerPredictionRepository.findByPoolIdAndUserId(poolId, user.getId());
        Optional<TournamentTopScorerResult> official = tournamentTopScorerResultRepository.findByTournamentId(tournamentId);
        return toResponse(poolId, tournamentId, prediction, official);
    }

    @Transactional
    public TopScorerResponse submit(UUID poolId, String userEmail, UUID teamId, UUID playerId, int goals) {
        if (!support.predictionOpen()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Top-scorer predictions are closed");
        }
        PredictionPool pool = support.requireTournamentPool(poolId);
        UserAccount user = support.requireMember(poolId, userEmail);
        UUID tournamentId = pool.getTournament().getId();
        Player player = support.resolvePlayer(tournamentId, teamId, playerId);
        Instant now = Instant.now(clock);

        TopScorerPrediction saved = topScorerPredictionRepository.findByPoolIdAndUserId(poolId, user.getId())
                .map(existing -> {
                    existing.resubmit(player, goals, now);
                    return topScorerPredictionRepository.save(existing);
                })
                .orElseGet(() -> topScorerPredictionRepository.save(new TopScorerPrediction(
                        pool, user, pool.getTournament(), player, goals, now
                )));

        Optional<TournamentTopScorerResult> official = tournamentTopScorerResultRepository.findByTournamentId(tournamentId);
        return toResponse(poolId, tournamentId, Optional.of(saved), official);
    }

    private TopScorerResponse toResponse(UUID poolId,
                                         UUID tournamentId,
                                         Optional<TopScorerPrediction> prediction,
                                         Optional<TournamentTopScorerResult> official) {
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
                prediction.map(TopScorerPrediction::getSubmittedAt).orElse(null),
                official.map(TournamentTopScorerResult::isConfirmed).orElse(false),
                official.filter(TournamentTopScorerResult::isConfirmed).map(this::toPick).orElse(null)
        );
    }

    private TopScorerPick toPick(TopScorerPrediction prediction) {
        return new TopScorerPick(prediction.getPlayer().getTeam().getId(), prediction.getPlayer().getId(), prediction.getPredictedGoals());
    }

    private TopScorerPick toPick(TournamentTopScorerResult result) {
        return new TopScorerPick(result.getPlayer().getTeam().getId(), result.getPlayer().getId(), result.getGoals());
    }
}
