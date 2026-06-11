package io.github.mathbteixeira.worldcuppredictionpool.finalranking.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Mutable projection holding the current points for one podium prediction. One
 * row per prediction keeps leaderboard aggregation cheap and lets repeated
 * confirmations overwrite rather than accumulate points.
 */
@Entity
@Table(name = "tournament_ranking_current_scores", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tournament_ranking_current_score_prediction", columnNames = {"prediction_id"})
})
public class TournamentRankingCurrentScore extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_id", nullable = false, unique = true)
    private TournamentRankingPrediction prediction;

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
    private Instant updatedAtScore;

    protected TournamentRankingCurrentScore() {
    }

    public TournamentRankingCurrentScore(TournamentRankingPrediction prediction,
                                         PredictionPool pool,
                                         UserAccount user,
                                         int pointsAwarded,
                                         int ruleVersion,
                                         String resultChecksum,
                                         Instant updatedAtScore) {
        this.prediction = prediction;
        this.pool = pool;
        this.user = user;
        this.pointsAwarded = pointsAwarded;
        this.ruleVersion = ruleVersion;
        this.resultChecksum = resultChecksum;
        this.updatedAtScore = updatedAtScore;
    }

    public void updateScore(int pointsAwarded, int ruleVersion, String resultChecksum, Instant updatedAtScore) {
        this.pointsAwarded = pointsAwarded;
        this.ruleVersion = ruleVersion;
        this.resultChecksum = resultChecksum;
        this.updatedAtScore = updatedAtScore;
    }

    public TournamentRankingPrediction getPrediction() {
        return prediction;
    }

    public PredictionPool getPool() {
        return pool;
    }

    public UserAccount getUser() {
        return user;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }
}