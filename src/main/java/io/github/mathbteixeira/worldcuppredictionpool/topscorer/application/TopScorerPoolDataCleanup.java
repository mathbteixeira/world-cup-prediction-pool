package io.github.mathbteixeira.worldcuppredictionpool.topscorer.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.application.PoolDataCleanupContributor;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence.TopScorerCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence.TopScorerPredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence.TopScorerScoreEventRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TopScorerPoolDataCleanup implements PoolDataCleanupContributor {

    private final TopScorerPredictionRepository topScorerPredictionRepository;
    private final TopScorerScoreEventRepository topScorerScoreEventRepository;
    private final TopScorerCurrentScoreRepository topScorerCurrentScoreRepository;

    public TopScorerPoolDataCleanup(TopScorerPredictionRepository topScorerPredictionRepository,
                                    TopScorerScoreEventRepository topScorerScoreEventRepository,
                                    TopScorerCurrentScoreRepository topScorerCurrentScoreRepository) {
        this.topScorerPredictionRepository = topScorerPredictionRepository;
        this.topScorerScoreEventRepository = topScorerScoreEventRepository;
        this.topScorerCurrentScoreRepository = topScorerCurrentScoreRepository;
    }

    @Override
    public void deletePoolData(UUID poolId) {
        topScorerScoreEventRepository.deleteByPoolId(poolId);
        topScorerCurrentScoreRepository.deleteByPoolId(poolId);
        topScorerPredictionRepository.deleteByPoolId(poolId);
    }

    @Override
    public void deleteUserPoolData(UUID poolId, UUID userId) {
        List<UUID> predictionIds = topScorerPredictionRepository.findIdsByPoolIdAndUserId(poolId, userId);
        if (predictionIds.isEmpty()) {
            return;
        }
        topScorerScoreEventRepository.deleteByPredictionIdIn(predictionIds);
        topScorerCurrentScoreRepository.deleteByPredictionIdIn(predictionIds);
        topScorerPredictionRepository.deleteByPoolIdAndUserId(poolId, userId);
    }
}
