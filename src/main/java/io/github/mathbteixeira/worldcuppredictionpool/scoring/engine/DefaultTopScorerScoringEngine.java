package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DefaultTopScorerScoringEngine implements TopScorerScoringEngine {

    @Override
    public TopScorerScoreBreakdown score(UUID predictedPlayerId,
                                         int predictedGoals,
                                         UUID actualPlayerId,
                                         int actualGoals,
                                         ScoringRuleDefinition rule) {
        boolean playerCorrect = predictedPlayerId.equals(actualPlayerId);
        int playerPoints = playerCorrect ? rule.topScorerPlayerPoints() : 0;
        int goalsPoints = playerCorrect && predictedGoals == actualGoals ? rule.topScorerGoalsPoints() : 0;
        int total = playerPoints + goalsPoints;
        String explanation = playerCorrect
                ? (goalsPoints > 0 ? "Top scorer and goals correct" : "Top scorer correct")
                : "Top scorer incorrect";
        return new TopScorerScoreBreakdown(total, playerPoints, goalsPoints, explanation);
    }
}
