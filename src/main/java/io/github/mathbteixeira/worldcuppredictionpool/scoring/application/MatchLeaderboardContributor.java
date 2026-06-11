package io.github.mathbteixeira.worldcuppredictionpool.scoring.application;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.PredictionCurrentScoreRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Contributes match score-prediction points (registered users and owner-managed
 * participants) to pool leaderboards, sourced from the match current-score
 * projection.
 */
@Component
public class MatchLeaderboardContributor implements LeaderboardPointContributor {

    private final PredictionCurrentScoreRepository predictionCurrentScoreRepository;

    public MatchLeaderboardContributor(PredictionCurrentScoreRepository predictionCurrentScoreRepository) {
        this.predictionCurrentScoreRepository = predictionCurrentScoreRepository;
    }

    @Override
    public List<ParticipantPoints> contributionsFor(Collection<UUID> poolIds) {
        List<ParticipantPoints> contributions = new ArrayList<>();
        for (PredictionCurrentScoreRepository.PoolUserTotalProjection aggregate
                : predictionCurrentScoreRepository.aggregateTotalsByPoolAndUser(poolIds)) {
            contributions.add(ParticipantPoints.forUser(
                    aggregate.getPoolId(),
                    aggregate.getUserId(),
                    Objects.requireNonNullElse(aggregate.getTotalPoints(), 0L)));
        }
        for (PredictionCurrentScoreRepository.PoolManagedParticipantTotalProjection aggregate
                : predictionCurrentScoreRepository.aggregateTotalsByPoolAndManagedParticipant(poolIds)) {
            contributions.add(ParticipantPoints.forManagedParticipant(
                    aggregate.getPoolId(),
                    aggregate.getManagedParticipantId(),
                    Objects.requireNonNullElse(aggregate.getTotalPoints(), 0L)));
        }
        return contributions;
    }
}