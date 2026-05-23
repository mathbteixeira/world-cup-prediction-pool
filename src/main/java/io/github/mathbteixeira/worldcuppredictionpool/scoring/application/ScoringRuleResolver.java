package io.github.mathbteixeira.worldcuppredictionpool.scoring.application;

import io.github.mathbteixeira.worldcuppredictionpool.scoring.engine.ScoringRuleDefinition;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.ScoringRuleRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScoringRuleResolver {

    private final ScoringRuleRepository scoringRuleRepository;

    public ScoringRuleResolver(ScoringRuleRepository scoringRuleRepository) {
        this.scoringRuleRepository = scoringRuleRepository;
    }

    public ScoringRuleDefinition resolve(UUID tournamentId) {
        return scoringRuleRepository.findTopByTournamentIdAndActiveTrueOrderByRuleVersionDesc(tournamentId)
                .map(rule -> new ScoringRuleDefinition(
                        rule.getRuleVersion(),
                        rule.getExactScorePoints(),
                        rule.getOutcomePoints(),
                        rule.getGoalDifferenceBonusPoints()))
                .orElseGet(ScoringRuleDefinition::defaultV1);
    }
}
