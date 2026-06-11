package io.github.mathbteixeira.worldcuppredictionpool.finalranking.application;

import io.github.mathbteixeira.worldcuppredictionpool.finalranking.api.TournamentRankingPicks;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.api.TournamentRankingResponse;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.domain.TournamentOfficialRanking;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.domain.TournamentRankingPrediction;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence.TournamentOfficialRankingRepository;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence.TournamentRankingPredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TeamSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
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
public class TournamentRankingPredictionService {

    private final PredictionPoolRepository predictionPoolRepository;
    private final PoolMembershipRepository poolMembershipRepository;
    private final UserAccountRepository userAccountRepository;
    private final TournamentRankingPredictionRepository tournamentRankingPredictionRepository;
    private final TournamentOfficialRankingRepository tournamentOfficialRankingRepository;
    private final TournamentRankingSupport support;
    private final Clock clock;

    public TournamentRankingPredictionService(PredictionPoolRepository predictionPoolRepository,
                                              PoolMembershipRepository poolMembershipRepository,
                                              UserAccountRepository userAccountRepository,
                                              TournamentRankingPredictionRepository tournamentRankingPredictionRepository,
                                              TournamentOfficialRankingRepository tournamentOfficialRankingRepository,
                                              TournamentRankingSupport support,
                                              Clock clock) {
        this.predictionPoolRepository = predictionPoolRepository;
        this.poolMembershipRepository = poolMembershipRepository;
        this.userAccountRepository = userAccountRepository;
        this.tournamentRankingPredictionRepository = tournamentRankingPredictionRepository;
        this.tournamentOfficialRankingRepository = tournamentOfficialRankingRepository;
        this.support = support;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TournamentRankingResponse getRanking(UUID poolId, String userEmail) {
        PredictionPool pool = requireTournamentPool(poolId);
        UserAccount user = requireMember(poolId, userEmail);
        UUID tournamentId = pool.getTournament().getId();

        Optional<TournamentRankingPrediction> prediction =
                tournamentRankingPredictionRepository.findByPoolIdAndUserId(poolId, user.getId());
        Optional<TournamentOfficialRanking> official =
                tournamentOfficialRankingRepository.findByTournamentId(tournamentId);

        return toResponse(poolId, tournamentId, prediction, official, Instant.now(clock));
    }

    @Transactional
    public TournamentRankingResponse submit(UUID poolId, String userEmail, TournamentRankingPicks picks) {
        PredictionPool pool = requireTournamentPool(poolId);
        UserAccount user = requireMember(poolId, userEmail);
        UUID tournamentId = pool.getTournament().getId();

        Instant now = Instant.now(clock);
        Instant deadline = support.predictionDeadline(tournamentId);
        if (deadline != null && !now.isBefore(deadline)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Final-ranking predictions are closed");
        }

        TournamentRankingSupport.ResolvedPodium resolved = support.resolvePodium(
                tournamentId,
                picks.championTeamId(),
                picks.runnerUpTeamId(),
                picks.thirdPlaceTeamId(),
                picks.fourthPlaceTeamId()
        );

        TournamentRankingPrediction saved = tournamentRankingPredictionRepository
                .findByPoolIdAndUserId(poolId, user.getId())
                .map(existing -> {
                    existing.resubmit(resolved.champion(), resolved.runnerUp(), resolved.third(), resolved.fourth(), now);
                    return tournamentRankingPredictionRepository.save(existing);
                })
                .orElseGet(() -> tournamentRankingPredictionRepository.save(new TournamentRankingPrediction(
                        pool,
                        user,
                        pool.getTournament(),
                        resolved.champion(),
                        resolved.runnerUp(),
                        resolved.third(),
                        resolved.fourth(),
                        now
                )));

        Optional<TournamentOfficialRanking> official =
                tournamentOfficialRankingRepository.findByTournamentId(tournamentId);
        return toResponse(poolId, tournamentId, Optional.of(saved), official, now);
    }

    private TournamentRankingResponse toResponse(UUID poolId,
                                                 UUID tournamentId,
                                                 Optional<TournamentRankingPrediction> prediction,
                                                 Optional<TournamentOfficialRanking> official,
                                                 Instant now) {
        List<TeamSummaryResponse> teams = support.teamsOf(tournamentId).stream()
                .map(team -> new TeamSummaryResponse(team.getId(), team.getName(), team.getFifaCode()))
                .toList();
        Instant deadline = support.predictionDeadline(tournamentId);
        boolean open = deadline == null || now.isBefore(deadline);

        return new TournamentRankingResponse(
                poolId,
                tournamentId,
                teams,
                deadline,
                open,
                prediction.map(this::toPicks).orElse(null),
                prediction.map(TournamentRankingPrediction::getSubmittedAt).orElse(null),
                official.map(TournamentOfficialRanking::isConfirmed).orElse(false),
                official.filter(TournamentOfficialRanking::isConfirmed).map(this::toPicks).orElse(null)
        );
    }

    private TournamentRankingPicks toPicks(TournamentRankingPrediction prediction) {
        return new TournamentRankingPicks(
                prediction.getChampionTeam().getId(),
                prediction.getRunnerUpTeam().getId(),
                prediction.getThirdPlaceTeam().getId(),
                prediction.getFourthPlaceTeam().getId()
        );
    }

    private TournamentRankingPicks toPicks(TournamentOfficialRanking ranking) {
        return new TournamentRankingPicks(
                ranking.getChampionTeam().getId(),
                ranking.getRunnerUpTeam().getId(),
                ranking.getThirdPlaceTeam().getId(),
                ranking.getFourthPlaceTeam().getId()
        );
    }

    private PredictionPool requireTournamentPool(UUID poolId) {
        PredictionPool pool = predictionPoolRepository.findById(poolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found"));
        if (pool.isSingleMatchPool()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Final-ranking predictions are available only for tournament pools");
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
