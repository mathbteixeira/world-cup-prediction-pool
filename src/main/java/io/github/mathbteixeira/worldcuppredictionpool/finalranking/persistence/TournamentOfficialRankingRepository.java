package io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.finalranking.domain.TournamentOfficialRanking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TournamentOfficialRankingRepository extends JpaRepository<TournamentOfficialRanking, UUID> {

    @EntityGraph(attributePaths = {"championTeam", "runnerUpTeam", "thirdPlaceTeam", "fourthPlaceTeam"})
    Optional<TournamentOfficialRanking> findByTournamentId(UUID tournamentId);
}