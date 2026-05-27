package io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.domain.ScoringRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScoringRuleRepository extends JpaRepository<ScoringRule, UUID> {

    Optional<ScoringRule> findTopByTournamentIdAndActiveTrueOrderByRuleVersionDesc(UUID tournamentId);
}
