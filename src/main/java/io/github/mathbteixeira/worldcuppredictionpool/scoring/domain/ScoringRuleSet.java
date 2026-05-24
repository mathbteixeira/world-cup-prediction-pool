package io.github.mathbteixeira.worldcuppredictionpool.scoring.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "scoring_rule_sets")
public class ScoringRuleSet extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false, unique = true)
    private Tournament tournament;

    @Column(nullable = false)
    private int exactScorePoints;

    @Column(nullable = false)
    private int outcomePoints;

    @Column(nullable = false)
    private int goalDifferencePoints;

    @Column(nullable = false)
    private int ruleVersion;

    protected ScoringRuleSet() {
    }
}
