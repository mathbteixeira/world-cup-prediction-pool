package io.github.mathbteixeira.worldcuppredictionpool.finalranking.application;

import io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence.TournamentRankingCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.LeaderboardPointContributor;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.ParticipantPoints;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Contributes tournament final-ranking (podium) prediction points to pool leaderboards. */
@Component
public class TournamentRankingLeaderboardContributor implements LeaderboardPointContributor {

    private final TournamentRankingCurrentScoreRepository tournamentRankingCurrentScoreRepository;

    public TournamentRankingLeaderboardContributor(TournamentRankingCurrentScoreRepository tournamentRankingCurrentScoreRepository) {
        this.tournamentRankingCurrentScoreRepository = tournamentRankingCurrentScoreRepository;
    }

    @Override
    public List<ParticipantPoints> contributionsFor(Collection<UUID> poolIds) {
        return tournamentRankingCurrentScoreRepository.aggregateTotalsByPoolAndUser(poolIds).stream()
                .map(aggregate -> ParticipantPoints.forUser(
                        aggregate.getPoolId(),
                        aggregate.getUserId(),
                        Objects.requireNonNullElse(aggregate.getTotalPoints(), 0L)))
                .toList();
    }
}