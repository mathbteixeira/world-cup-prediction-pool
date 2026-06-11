package io.github.mathbteixeira.worldcuppredictionpool.topscorer.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Player;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.PlayerRepository;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class TopScorerSupport {

    static final Instant PREDICTION_DEADLINE = Instant.parse("2026-06-21T00:00:00Z");

    private final PredictionPoolRepository predictionPoolRepository;
    private final PoolMembershipRepository poolMembershipRepository;
    private final UserAccountRepository userAccountRepository;
    private final PlayerRepository playerRepository;
    private final Clock clock;

    public TopScorerSupport(PredictionPoolRepository predictionPoolRepository,
                            PoolMembershipRepository poolMembershipRepository,
                            UserAccountRepository userAccountRepository,
                            PlayerRepository playerRepository,
                            Clock clock) {
        this.predictionPoolRepository = predictionPoolRepository;
        this.poolMembershipRepository = poolMembershipRepository;
        this.userAccountRepository = userAccountRepository;
        this.playerRepository = playerRepository;
        this.clock = clock;
    }

    PredictionPool requireTournamentPool(UUID poolId) {
        PredictionPool pool = predictionPoolRepository.findById(poolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found"));
        if (pool.isSingleMatchPool()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Top-scorer predictions are available only for tournament pools");
        }
        return pool;
    }

    UserAccount requireMember(UUID poolId, String userEmail) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (poolMembershipRepository.findByPoolIdAndUserId(poolId, user.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this pool");
        }
        return user;
    }

    Player resolvePlayer(UUID tournamentId, UUID teamId, UUID playerId) {
        Player player = playerRepository.findByIdAndTeamTournamentId(playerId, tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player does not belong to the tournament"));
        if (!player.getTeam().getId().equals(teamId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player does not belong to the selected team");
        }
        return player;
    }

    Player resolvePlayer(UUID tournamentId, UUID playerId) {
        return playerRepository.findByIdAndTeamTournamentId(playerId, tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player does not belong to the tournament"));
    }

    Instant predictionDeadline() {
        return PREDICTION_DEADLINE;
    }

    boolean predictionOpen() {
        return Instant.now(clock).isBefore(PREDICTION_DEADLINE);
    }
}
