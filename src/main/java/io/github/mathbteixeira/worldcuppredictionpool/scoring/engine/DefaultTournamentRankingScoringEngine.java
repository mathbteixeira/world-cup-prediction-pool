package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Awards position-specific points when the user places a team in its exact final
 * tournament position: champion, runner-up, third and fourth place each carry
 * their own award from the active scoring rule. Misplaced teams score nothing.
 */
@Component
public class DefaultTournamentRankingScoringEngine implements TournamentRankingScoringEngine {

    @Override
    public TournamentRankingScoreBreakdown score(List<UUID> predictedOrder, List<UUID> actualOrder, ScoringRuleDefinition rule) {
        if (predictedOrder == null || predictedOrder.isEmpty()) {
            return TournamentRankingScoreBreakdown.noPrediction();
        }

        int champion = awardFor(0, predictedOrder, actualOrder, rule.championPoints());
        int runnerUp = awardFor(1, predictedOrder, actualOrder, rule.runnerUpPoints());
        int third = awardFor(2, predictedOrder, actualOrder, rule.thirdPlacePoints());
        int fourth = awardFor(3, predictedOrder, actualOrder, rule.fourthPlacePoints());

        int total = champion + runnerUp + third + fourth;
        int correctPositions = (champion > 0 ? 1 : 0) + (runnerUp > 0 ? 1 : 0)
                + (third > 0 ? 1 : 0) + (fourth > 0 ? 1 : 0);
        String explanation = correctPositions == 0
                ? "No correct final positions"
                : correctPositions + " correct final position(s)";
        return new TournamentRankingScoreBreakdown(total, champion, runnerUp, third, fourth, explanation);
    }

    private int awardFor(int index, List<UUID> predictedOrder, List<UUID> actualOrder, int points) {
        if (index >= predictedOrder.size() || index >= actualOrder.size()) {
            return 0;
        }
        UUID predicted = predictedOrder.get(index);
        return predicted != null && predicted.equals(actualOrder.get(index)) ? points : 0;
    }
}