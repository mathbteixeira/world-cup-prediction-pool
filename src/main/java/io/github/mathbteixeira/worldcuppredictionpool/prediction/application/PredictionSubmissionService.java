package io.github.mathbteixeira.worldcuppredictionpool.prediction.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence.PredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;

@Service
public class PredictionSubmissionService {

    private final PredictionRepository predictionRepository;
    private final PredictionPoolRepository predictionPoolRepository;
    private final MatchRepository matchRepository;
    private final UserAccountRepository userAccountRepository;
    private final Clock clock;

    public PredictionSubmissionService(PredictionRepository predictionRepository,
                                       PredictionPoolRepository predictionPoolRepository,
                                       MatchRepository matchRepository,
                                       UserAccountRepository userAccountRepository,
                                       Clock clock) {
        this.predictionRepository = predictionRepository;
        this.predictionPoolRepository = predictionPoolRepository;
        this.matchRepository = matchRepository;
        this.userAccountRepository = userAccountRepository;
        this.clock = clock;
    }

    @Transactional
    public Prediction submit(SubmitPredictionCommand command) {
        PredictionPool pool = predictionPoolRepository.findById(command.poolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found"));
        Match match = matchRepository.findById(command.matchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));

        Instant now = Instant.now(clock);
        if (!match.canAcceptPredictionsAt(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Predictions are closed for this match");
        }

        UserAccount user = userAccountRepository.findByEmailIgnoreCase(command.userEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

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
}
