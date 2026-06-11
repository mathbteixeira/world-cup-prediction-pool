package io.github.mathbteixeira.worldcuppredictionpool.groupstage.application;

import io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence.GroupStandingCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence.GroupStandingPredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence.GroupStandingScoreEventRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.application.PoolDataCleanupContributor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Removes group-position prediction data when pools or members are deleted. */
@Component
public class GroupStandingPoolDataCleanup implements PoolDataCleanupContributor {

    private final GroupStandingPredictionRepository groupStandingPredictionRepository;
    private final GroupStandingScoreEventRepository groupStandingScoreEventRepository;
    private final GroupStandingCurrentScoreRepository groupStandingCurrentScoreRepository;

    public GroupStandingPoolDataCleanup(GroupStandingPredictionRepository groupStandingPredictionRepository,
                                        GroupStandingScoreEventRepository groupStandingScoreEventRepository,
                                        GroupStandingCurrentScoreRepository groupStandingCurrentScoreRepository) {
        this.groupStandingPredictionRepository = groupStandingPredictionRepository;
        this.groupStandingScoreEventRepository = groupStandingScoreEventRepository;
        this.groupStandingCurrentScoreRepository = groupStandingCurrentScoreRepository;
    }

    @Override
    public void deletePoolData(UUID poolId) {
        groupStandingScoreEventRepository.deleteByPoolId(poolId);
        groupStandingCurrentScoreRepository.deleteByPoolId(poolId);
        groupStandingPredictionRepository.deleteByPoolId(poolId);
    }

    @Override
    public void deleteUserPoolData(UUID poolId, UUID userId) {
        List<UUID> predictionIds = groupStandingPredictionRepository.findIdsByPoolIdAndUserId(poolId, userId);
        if (predictionIds.isEmpty()) {
            return;
        }
        groupStandingScoreEventRepository.deleteByPredictionIdIn(predictionIds);
        groupStandingCurrentScoreRepository.deleteByPredictionIdIn(predictionIds);
        groupStandingPredictionRepository.deleteByPoolIdAndUserId(poolId, userId);
    }
}