package io.github.mathbteixeira.worldcuppredictionpool.groupstage.domain;

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
 * Mutable projection holding the current points for one group-position
 * prediction. One row per prediction keeps leaderboard aggregation cheap and
 * lets repeated confirmations overwrite rather than accumulate points.
 */
@Entity
@Table(name = "group_standing_current_scores", uniqueConstraints = {
        @UniqueConstraint(name = "uk_group_standing_current_score_prediction", columnNames = {"prediction_id"})
})
public class GroupStandingCurrentScore extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_id", nullable = false, unique = true)
    private GroupStandingPrediction prediction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private PredictionPool pool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "group_name", nullable = false, length = 1)
    private String groupName;

    @Column(nullable = false)
    private int pointsAwarded;

    @Column(nullable = false)
    private int ruleVersion;

    @Column(nullable = false, length = 120)
    private String resultChecksum;

    @Column(nullable = false)
    private Instant updatedAtScore;

    protected GroupStandingCurrentScore() {
    }

    public GroupStandingCurrentScore(GroupStandingPrediction prediction,
                                     PredictionPool pool,
                                     UserAccount user,
                                     String groupName,
                                     int pointsAwarded,
                                     int ruleVersion,
                                     String resultChecksum,
                                     Instant updatedAtScore) {
        this.prediction = prediction;
        this.pool = pool;
        this.user = user;
        this.groupName = groupName;
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

    public GroupStandingPrediction getPrediction() {
        return prediction;
    }

    public PredictionPool getPool() {
        return pool;
    }

    public UserAccount getUser() {
        return user;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }
}