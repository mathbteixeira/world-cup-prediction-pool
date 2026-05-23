package io.github.mathbteixeira.worldcuppredictionpool.tournament.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "match_results")
public class MatchResult extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private Match match;

    @Column(nullable = false)
    private int homeScore;

    @Column(nullable = false)
    private int awayScore;

    private Integer homePenaltyScore;

    private Integer awayPenaltyScore;

    @Column(nullable = false)
    private boolean finalResult;

    @Column(nullable = false)
    private Instant finalizedAt;

    @Column(nullable = false, length = 120)
    private String resultChecksum;

    protected MatchResult() {
    }

    public MatchResult(Match match,
                       int homeScore,
                       int awayScore,
                       Integer homePenaltyScore,
                       Integer awayPenaltyScore,
                       boolean finalResult,
                       Instant finalizedAt,
                       String resultChecksum) {
        this.match = match;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.homePenaltyScore = homePenaltyScore;
        this.awayPenaltyScore = awayPenaltyScore;
        this.finalResult = finalResult;
        this.finalizedAt = finalizedAt;
        this.resultChecksum = resultChecksum;
    }

    public void updateResult(int homeScore,
                             int awayScore,
                             Integer homePenaltyScore,
                             Integer awayPenaltyScore,
                             boolean finalResult,
                             Instant finalizedAt,
                             String resultChecksum) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.homePenaltyScore = homePenaltyScore;
        this.awayPenaltyScore = awayPenaltyScore;
        this.finalResult = finalResult;
        this.finalizedAt = finalizedAt;
        this.resultChecksum = resultChecksum;
    }

    public Match getMatch() {
        return match;
    }

    public int getHomeScore() {
        return homeScore;
    }

    public int getAwayScore() {
        return awayScore;
    }

    public Integer getHomePenaltyScore() {
        return homePenaltyScore;
    }

    public Integer getAwayPenaltyScore() {
        return awayPenaltyScore;
    }

    public boolean isFinalResult() {
        return finalResult;
    }

    public Instant getFinalizedAt() {
        return finalizedAt;
    }

    public String getResultChecksum() {
        return resultChecksum;
    }
}
