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
        this.tournament = tournament;
        this.exactScorePoints = exactScorePoints;
        this.outcomePoints = outcomePoints;
        this.goalDifferenceBonusPoints = goalDifferenceBonusPoints;
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

    public int getRuleVersion() {
        return ruleVersion;
    }

    public boolean isActive() {
        return active;
    }
}
