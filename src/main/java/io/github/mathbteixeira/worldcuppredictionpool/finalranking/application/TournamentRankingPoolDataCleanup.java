package io.github.mathbteixeira.worldcuppredictionpool.finalranking.application;

import io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence.TournamentRankingCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence.TournamentRankingPredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence.TournamentRankingScoreEventRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.application.PoolDataCleanupContributor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Removes podium prediction data when pools or members are deleted. */
@Component
public class TournamentRankingPoolDataCleanup implements PoolDataCleanupContributor {

    private final TournamentRankingPredictionRepository tournamentRankingPredictionRepository;
    private final TournamentRankingScoreEventRepository tournamentRankingScoreEventRepository;
    private final TournamentRankingCurrentScoreRepository tournamentRankingCurrentScoreRepository;

    public TournamentRankingPoolDataCleanup(TournamentRankingPredictionRepository tournamentRankingPredictionRepository,
                                            TournamentRankingScoreEventRepository tournamentRankingScoreEventRepository,
                                            TournamentRankingCurrentScoreRepository tournamentRankingCurrentScoreRepository) {
        this.tournamentRankingPredictionRepository = tournamentRankingPredictionRepository;
        this.tournamentRankingScoreEventRepository = tournamentRankingScoreEventRepository;
        this.tournamentRankingCurrentScoreRepository = tournamentRankingCurrentScoreRepository;
    }

    @Override
    public void deletePoolData(UUID poolId) {
        tournamentRankingScoreEventRepository.deleteByPoolId(poolId);
        tournamentRankingCurrentScoreRepository.deleteByPoolId(poolId);
        tournamentRankingPredictionRepository.deleteByPoolId(poolId);
    }

    @Override
    public void deleteUserPoolData(UUID poolId, UUID userId) {
        List<UUID> predictionIds = tournamentRankingPredictionRepository.findIdsByPoolIdAndUserId(poolId, userId);
        if (predictionIds.isEmpty()) {
            return;
        }
        tournamentRankingScoreEventRepository.deleteByPredictionIdIn(predictionIds);
        tournamentRankingCurrentScoreRepository.deleteByPredictionIdIn(predictionIds);
        tournamentRankingPredictionRepository.deleteByPoolIdAndUserId(poolId, userId);
    }
}