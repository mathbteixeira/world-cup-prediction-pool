package io.github.mathbteixeira.worldcuppredictionpool.groupstage.application;

import io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence.GroupStandingCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.LeaderboardPointContributor;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.ParticipantPoints;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Contributes group-position prediction points to pool leaderboards. */
@Component
public class GroupStandingLeaderboardContributor implements LeaderboardPointContributor {

    private final GroupStandingCurrentScoreRepository groupStandingCurrentScoreRepository;

    public GroupStandingLeaderboardContributor(GroupStandingCurrentScoreRepository groupStandingCurrentScoreRepository) {
        this.groupStandingCurrentScoreRepository = groupStandingCurrentScoreRepository;
    }

    @Override
    public List<ParticipantPoints> contributionsFor(Collection<UUID> poolIds) {
        return groupStandingCurrentScoreRepository.aggregateTotalsByPoolAndUser(poolIds).stream()
                .map(aggregate -> ParticipantPoints.forUser(
                        aggregate.getPoolId(),
                        aggregate.getUserId(),
                        Objects.requireNonNullElse(aggregate.getTotalPoints(), 0L)))
                .toList();
    }
}