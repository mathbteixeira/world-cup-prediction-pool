package io.github.mathbteixeira.worldcuppredictionpool.scoring.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "score_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_score_event_prediction_checksum", columnNames = {"prediction_id", "result_checksum"})
})
public class ScoreEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private PredictionPool pool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_id", nullable = false)
    private Prediction prediction;

    @Column(nullable = false)
    private int pointsAwarded;

    @Column(nullable = false)
    private int exactScorePointsAwarded;

    @Column(nullable = false)
    private int outcomePointsAwarded;

    @Column(nullable = false)
    private int goalDifferenceBonusPointsAwarded;

    @Column(nullable = false, length = 255)
    private String explanation;

    @Column(nullable = false)
    private int ruleVersion;

    @Column(nullable = false, length = 120)
    private String resultChecksum;

    @Column(nullable = false)
    private Instant calculatedAt;

    protected ScoreEvent() {
    }

    public ScoreEvent(PredictionPool pool,
                      UserAccount user,
                      Match match,
                      Prediction prediction,
                      int pointsAwarded,
                      int exactScorePointsAwarded,
                      int outcomePointsAwarded,
                      int goalDifferenceBonusPointsAwarded,
                      String explanation,
                      int ruleVersion,
                      String resultChecksum,
                      Instant calculatedAt) {
        this.pool = pool;
        this.user = user;
        this.match = match;
        this.prediction = prediction;
        this.pointsAwarded = pointsAwarded;
        this.exactScorePointsAwarded = exactScorePointsAwarded;
        this.outcomePointsAwarded = outcomePointsAwarded;
        this.goalDifferenceBonusPointsAwarded = goalDifferenceBonusPointsAwarded;
        this.explanation = explanation;
        this.ruleVersion = ruleVersion;
        this.resultChecksum = resultChecksum;
        this.calculatedAt = calculatedAt;
    }

    public PredictionPool getPool() {
        return pool;
    }

    public UserAccount getUser() {
        return user;
    }

    public Match getMatch() {
        return match;
    }

    public Prediction getPrediction() {
        return prediction;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }

    public int getRuleVersion() {
        return ruleVersion;
    }

    public String getResultChecksum() {
        return resultChecksum;
    }
}
