package io.github.mathbteixeira.worldcuppredictionpool.topscorer.application;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.LeaderboardPointContributor;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.ParticipantPoints;
import io.github.mathbteixeira.worldcuppredictionpool.topscorer.persistence.TopScorerCurrentScoreRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class TopScorerLeaderboardContributor implements LeaderboardPointContributor {

    private final TopScorerCurrentScoreRepository topScorerCurrentScoreRepository;

    public TopScorerLeaderboardContributor(TopScorerCurrentScoreRepository topScorerCurrentScoreRepository) {
        this.topScorerCurrentScoreRepository = topScorerCurrentScoreRepository;
    }

    @Override
    public List<ParticipantPoints> contributionsFor(Collection<UUID> poolIds) {
        return topScorerCurrentScoreRepository.aggregateTotalsByPoolAndUser(poolIds).stream()
                .map(aggregate -> ParticipantPoints.forUser(
                        aggregate.getPoolId(),
                        aggregate.getUserId(),
                        Objects.requireNonNullElse(aggregate.getTotalPoints(), 0L)))
                .toList();
    }
}
