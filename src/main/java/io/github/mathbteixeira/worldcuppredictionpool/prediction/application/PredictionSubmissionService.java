package io.github.mathbteixeira.worldcuppredictionpool.prediction.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.api.PoolPredictionResponse;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.api.PredictionUserResponse;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence.PredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.MatchResultResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.MatchSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TeamSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchResult;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchResultRepository;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
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
public class PredictionSubmissionService {

    private final PredictionRepository predictionRepository;
    private final PredictionPoolRepository predictionPoolRepository;
    private final PoolMembershipRepository poolMembershipRepository;
    private final MatchRepository matchRepository;
    private final MatchResultRepository matchResultRepository;
    private final UserAccountRepository userAccountRepository;
    private final Clock clock;

    public PredictionSubmissionService(PredictionRepository predictionRepository,
                                       PredictionPoolRepository predictionPoolRepository,
                                       PoolMembershipRepository poolMembershipRepository,
                                       MatchRepository matchRepository,
                                       MatchResultRepository matchResultRepository,
                                       UserAccountRepository userAccountRepository,
                                       Clock clock) {
        this.predictionRepository = predictionRepository;
        this.predictionPoolRepository = predictionPoolRepository;
        this.poolMembershipRepository = poolMembershipRepository;
        this.matchRepository = matchRepository;
        this.matchResultRepository = matchResultRepository;
        this.userAccountRepository = userAccountRepository;
        this.clock = clock;
    }

    @Transactional
    public Prediction submit(SubmitPredictionCommand command) {
        PredictionPool pool = predictionPoolRepository.findById(command.poolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found"));
        Match match = matchRepository.findById(command.matchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));
        if (pool.isSingleMatchPool() && !pool.getSingleMatch().getId().equals(match.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match does not belong to single-match pool");
        }
        if (!pool.isSingleMatchPool() && !pool.getTournament().getId().equals(match.getTournament().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Match does not belong to pool tournament");
        }

        Instant now = Instant.now(clock);
        if (!match.canAcceptPredictionsAt(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Predictions are closed for this match");
        }

        UserAccount user = userAccountRepository.findByEmailIgnoreCase(command.userEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (poolMembershipRepository.findByPoolIdAndUserId(command.poolId(), user.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this pool");
        }

        return predictionRepository.findByPoolIdAndMatchIdAndUserId(command.poolId(), command.matchId(), user.getId())
                .map(existing -> {
                    existing.resubmit(command.homeScore(), command.awayScore(), now);
                    return predictionRepository.save(existing);
                })
                .orElseGet(() -> predictionRepository.save(new Prediction(
                        pool,
                        match,
                        user,
                        command.homeScore(),
                        command.awayScore(),
                        now
                )));
    }

    @Transactional(readOnly = true)
    public List<PoolPredictionResponse> listVisiblePoolPredictions(UUID poolId, String userEmail) {
        predictionPoolRepository.findById(poolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found"));
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (poolMembershipRepository.findByPoolIdAndUserId(poolId, user.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this pool");
        }

        Instant now = Instant.now(clock);
        List<Prediction> predictions = predictionRepository.findAllForPoolOrderByKickoffAt(poolId).stream()
                .filter(prediction -> isVisibleToUser(prediction, user, now))
                .toList();
        if (predictions.isEmpty()) {
            return List.of();
        }

        Map<UUID, MatchResult> resultsByMatchId = matchResultRepository.findAllByMatchIdIn(
                        predictions.stream().map(prediction -> prediction.getMatch().getId()).toList())
                .stream()
                .collect(Collectors.toMap(result -> result.getMatch().getId(), Function.identity()));

        return predictions.stream()
                .map(prediction -> toResponse(
                        prediction,
                        user,
                        Optional.ofNullable(resultsByMatchId.get(prediction.getMatch().getId())),
                        now))
                .toList();
    }

    private boolean isVisibleToUser(Prediction prediction, UserAccount user, Instant now) {
        return prediction.getUser().getId().equals(user.getId())
                || !prediction.getMatch().canAcceptPredictionsAt(now);
    }

    private PoolPredictionResponse toResponse(Prediction prediction,
                                              UserAccount currentUser,
                                              Optional<MatchResult> result,
                                              Instant now) {
        UserAccount predictionUser = prediction.getUser();
        return new PoolPredictionResponse(
                prediction.getId(),
                prediction.getPool().getId(),
                new PredictionUserResponse(predictionUser.getId(), predictionUser.getUsername()),
                predictionUser.getId().equals(currentUser.getId()),
                toMatchResponse(prediction.getMatch(), result, now),
                prediction.getPredictedHomeScore(),
                prediction.getPredictedAwayScore(),
                prediction.getSubmittedAt()
        );
    }

    private MatchSummaryResponse toMatchResponse(Match match, Optional<MatchResult> result, Instant now) {
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
        if (team == null) {
            return null;
        }
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
