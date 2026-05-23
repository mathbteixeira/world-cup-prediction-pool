package io.github.mathbteixeira.worldcuppredictionpool.scoring.engine;

import org.springframework.stereotype.Component;

@Component
public class DefaultPredictionScoringEngine implements PredictionScoringEngine {

    @Override
    public ScoreBreakdown score(PredictionScoreInput prediction, MatchScoreInput actualResult, ScoringRuleDefinition rule) {
        if (prediction == null) {
            return ScoreBreakdown.noPrediction();
        }

        boolean exactScore = prediction.homeScore() == actualResult.homeScore()
                && prediction.awayScore() == actualResult.awayScore();

        boolean correctOutcome = outcome(prediction.homeScore(), prediction.awayScore())
                == outcome(actualResult.homeScore(), actualResult.awayScore());

        boolean goalDifferenceMatches = (prediction.homeScore() - prediction.awayScore())
                == (actualResult.homeScore() - actualResult.awayScore());

        int exactPoints = 0;
        int outcomePoints = 0;
        int goalDifferenceBonus = 0;

        String explanation;
        if (exactScore) {
            exactPoints = rule.exactScorePoints();
            explanation = "Exact score predicted";
        } else if (correctOutcome && goalDifferenceMatches) {
            outcomePoints = rule.outcomePoints();
            goalDifferenceBonus = rule.goalDifferenceBonusPoints();
            explanation = "Correct outcome and goal difference";
        } else if (correctOutcome) {
            outcomePoints = rule.outcomePoints();
            explanation = "Correct outcome";
        } else {
            explanation = "Wrong prediction";
        }

        int total = exactPoints + outcomePoints + goalDifferenceBonus;
        return new ScoreBreakdown(total, exactPoints, outcomePoints, goalDifferenceBonus, explanation);
    }

    private int outcome(int homeScore, int awayScore) {
        return Integer.compare(homeScore, awayScore);
    }
}
