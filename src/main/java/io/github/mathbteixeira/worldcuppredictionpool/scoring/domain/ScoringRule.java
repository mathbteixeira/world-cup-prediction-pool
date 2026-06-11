package io.github.mathbteixeira.worldcuppredictionpool.scoring.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "scoring_rules", uniqueConstraints = {
        @UniqueConstraint(name = "uk_scoring_rule_tournament_version", columnNames = {"tournament_id", "rule_version"})
})
public class ScoringRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(nullable = false)
    private int exactScorePoints;

    @Column(nullable = false)
    private int outcomePoints;

    @Column(nullable = false)
    private int goalDifferenceBonusPoints;

    @Column(nullable = false)
    private int groupPositionPoints;

    @Column(nullable = false)
    private int championPoints;

    @Column(nullable = false)
    private int runnerUpPoints;

    @Column(nullable = false)
    private int thirdPlacePoints;

    @Column(nullable = false)
    private int fourthPlacePoints;

    @Column(nullable = false)
    private int topScorerPlayerPoints;

    @Column(nullable = false)
    private int topScorerGoalsPoints;

    @Column(nullable = false)
    private int ruleVersion;

    @Column(nullable = false)
    private boolean active;

    protected ScoringRule() {
    }

    public ScoringRule(Tournament tournament,
                       int exactScorePoints,
                       int outcomePoints,
                       int goalDifferenceBonusPoints,
                       int ruleVersion,
                       boolean active) {
        this(tournament, exactScorePoints, outcomePoints, goalDifferenceBonusPoints, 10, 20, 18, 15, 15, 20, 10, ruleVersion, active);
    }

    public ScoringRule(Tournament tournament,
                       int exactScorePoints,
                       int outcomePoints,
                       int goalDifferenceBonusPoints,
                       int groupPositionPoints,
                       int championPoints,
                       int runnerUpPoints,
                       int thirdPlacePoints,
                       int fourthPlacePoints,
                       int ruleVersion,
                       boolean active) {
        this(tournament, exactScorePoints, outcomePoints, goalDifferenceBonusPoints, groupPositionPoints, championPoints,
                runnerUpPoints, thirdPlacePoints, fourthPlacePoints, 20, 10, ruleVersion, active);
    }

    public ScoringRule(Tournament tournament,
                       int exactScorePoints,
                       int outcomePoints,
                       int goalDifferenceBonusPoints,
                       int groupPositionPoints,
                       int championPoints,
                       int runnerUpPoints,
                       int thirdPlacePoints,
                       int fourthPlacePoints,
                       int topScorerPlayerPoints,
                       int topScorerGoalsPoints,
                       int ruleVersion,
                       boolean active) {
        this.tournament = tournament;
        this.exactScorePoints = exactScorePoints;
        this.outcomePoints = outcomePoints;
        this.goalDifferenceBonusPoints = goalDifferenceBonusPoints;
        this.groupPositionPoints = groupPositionPoints;
        this.championPoints = championPoints;
        this.runnerUpPoints = runnerUpPoints;
        this.thirdPlacePoints = thirdPlacePoints;
        this.fourthPlacePoints = fourthPlacePoints;
        this.topScorerPlayerPoints = topScorerPlayerPoints;
        this.topScorerGoalsPoints = topScorerGoalsPoints;
        this.ruleVersion = ruleVersion;
        this.active = active;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public int getExactScorePoints() {
        return exactScorePoints;
    }

    public int getOutcomePoints() {
        return outcomePoints;
    }

    public int getGoalDifferenceBonusPoints() {
        return goalDifferenceBonusPoints;
    }

    public int getGroupPositionPoints() {
        return groupPositionPoints;
    }

    public int getChampionPoints() {
        return championPoints;
    }

    public int getRunnerUpPoints() {
        return runnerUpPoints;
    }

    public int getThirdPlacePoints() {
        return thirdPlacePoints;
    }

    public int getFourthPlacePoints() {
        return fourthPlacePoints;
    }

    public int getTopScorerPlayerPoints() {
        return topScorerPlayerPoints;
    }

    public int getTopScorerGoalsPoints() {
        return topScorerGoalsPoints;
    }

    public int getRuleVersion() {
        return ruleVersion;
    }

    public boolean isActive() {
        return active;
    }
}
