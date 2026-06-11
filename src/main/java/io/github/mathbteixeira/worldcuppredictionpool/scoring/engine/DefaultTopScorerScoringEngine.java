package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

import org.springframework.stereotype.Component;

@Component
public class DefaultTopScorerScoringEngine implements TopScorerScoringEngine {

    @Override
    public TopScorerScoreBreakdown score(boolean playerCorrect,
                                         boolean goalsCorrect,
                                         ScoringRuleDefinition rule) {
        int playerPoints = playerCorrect ? rule.topScorerPlayerPoints() : 0;
        int goalsPoints = playerCorrect && goalsCorrect ? rule.topScorerGoalsPoints() : 0;
        int total = playerPoints + goalsPoints;
        String explanation = playerCorrect
                ? (goalsPoints > 0 ? "Top scorer and goals correct" : "Top scorer correct")
                : "Top scorer incorrect";
        return new TopScorerScoreBreakdown(total, playerPoints, goalsPoints, explanation);
    }
}
