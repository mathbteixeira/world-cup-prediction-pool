package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Awards {@code groupPositionPoints} for every team the user placed in its
 * correct final group position, and nothing for misplaced teams.
 */
@Component
public class DefaultGroupStandingScoringEngine implements GroupStandingScoringEngine {

    @Override
    public GroupStandingScoreBreakdown score(List<UUID> predictedOrder, List<UUID> actualOrder, ScoringRuleDefinition rule) {
        if (predictedOrder == null || predictedOrder.isEmpty()) {
            return GroupStandingScoreBreakdown.noPrediction();
        }

        int correctPositions = 0;
        int positions = Math.min(predictedOrder.size(), actualOrder.size());
        for (int index = 0; index < positions; index++) {
            UUID predicted = predictedOrder.get(index);
            if (predicted != null && predicted.equals(actualOrder.get(index))) {
                correctPositions++;
            }
        }

        int total = correctPositions * rule.groupPositionPoints();
        String explanation = correctPositions == 0
                ? "No correct group positions"
                : correctPositions + " correct group position(s)";
        return new GroupStandingScoreBreakdown(total, correctPositions, explanation);
    }
}