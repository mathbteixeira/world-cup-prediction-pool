package io.github.mathbteixeira.worldcuppredictionpool.groupstage.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Immutable, append-only audit record of the points a single group-position
 * prediction earned for a specific confirmed standings snapshot. Uniqueness on
 * {@code (prediction_id, result_checksum)} makes re-running the confirmation
 * idempotent: the same snapshot never inserts a second event.
 */
@Entity
@Table(name = "group_standing_score_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_group_standing_score_event", columnNames = {"prediction_id", "result_checksum"})
})
public class GroupStandingScoreEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private PredictionPool pool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_id", nullable = false)
    private GroupStandingPrediction prediction;

    @Column(name = "group_name", nullable = false, length = 1)
    private String groupName;

    @Column(nullable = false)
    private int pointsAwarded;

    @Column(nullable = false)
    private int correctPositions;

    @Column(nullable = false, length = 255)
    private String explanation;

    @Column(nullable = false)
    private int ruleVersion;

    @Column(nullable = false, length = 120)
    private String resultChecksum;

    @Column(nullable = false)
    private Instant calculatedAt;

    protected GroupStandingScoreEvent() {
    }

    public GroupStandingScoreEvent(PredictionPool pool,
                                   UserAccount user,
                                   Tournament tournament,
                                   GroupStandingPrediction prediction,
                                   String groupName,
                                   int pointsAwarded,
                                   int correctPositions,
                                   String explanation,
                                   int ruleVersion,
                                   String resultChecksum,
                                   Instant calculatedAt) {
        this.pool = pool;
        this.user = user;
        this.tournament = tournament;
        this.prediction = prediction;
        this.groupName = groupName;
        this.pointsAwarded = pointsAwarded;
        this.correctPositions = correctPositions;
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

    public GroupStandingPrediction getPrediction() {
        return prediction;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }

    public int getCorrectPositions() {
        return correctPositions;
    }

    public int getRuleVersion() {
        return ruleVersion;
    }

    public String getResultChecksum() {
        return resultChecksum;
    }
}