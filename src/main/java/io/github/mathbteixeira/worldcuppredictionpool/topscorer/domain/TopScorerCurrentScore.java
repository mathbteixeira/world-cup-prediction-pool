package io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "top_scorer_current_scores")
public class TopScorerCurrentScore extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_id", nullable = false, unique = true)
    private TopScorerPrediction prediction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private PredictionPool pool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false)
    private int pointsAwarded;

    @Column(nullable = false)
    private int ruleVersion;

    @Column(nullable = false, length = 120)
    private String resultChecksum;

    @Column(nullable = false)
    private Instant recalculatedAt;

    protected TopScorerCurrentScore() {
    }

    public TopScorerCurrentScore(TopScorerPrediction prediction, PredictionPool pool, UserAccount user, int pointsAwarded, int ruleVersion, String resultChecksum, Instant recalculatedAt) {
        this.prediction = prediction;
        this.pool = pool;
        this.user = user;
        this.pointsAwarded = pointsAwarded;
        this.ruleVersion = ruleVersion;
        this.resultChecksum = resultChecksum;
        this.recalculatedAt = recalculatedAt;
    }

    public void updateScore(int pointsAwarded, int ruleVersion, String resultChecksum, Instant recalculatedAt) {
        this.pointsAwarded = pointsAwarded;
        this.ruleVersion = ruleVersion;
        this.resultChecksum = resultChecksum;
        this.recalculatedAt = recalculatedAt;
    }
}
