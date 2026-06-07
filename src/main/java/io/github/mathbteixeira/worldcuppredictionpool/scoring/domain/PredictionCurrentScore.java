package io.github.mathbteixeira.worldcuppredictionpool.scoring.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.ManagedParticipant;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
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

@Entity
@Table(name = "prediction_current_scores", uniqueConstraints = {
        @UniqueConstraint(name = "uk_prediction_current_score_prediction", columnNames = {"prediction_id"})
})
public class PredictionCurrentScore extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_id", nullable = false, unique = true)
    private Prediction prediction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private PredictionPool pool;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_participant_id")
    private ManagedParticipant managedParticipant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false)
    private int pointsAwarded;

    @Column(nullable = false)
    private int ruleVersion;

    @Column(nullable = false, length = 120)
    private String resultChecksum;

    @Column(nullable = false)
    private Instant updatedAtScore;

    protected PredictionCurrentScore() {
    }

    public PredictionCurrentScore(Prediction prediction,
                                  PredictionPool pool,
                                  UserAccount user,
                                  Match match,
                                  int pointsAwarded,
                                  int ruleVersion,
                                  String resultChecksum,
                                  Instant updatedAtScore) {
        this.prediction = prediction;
        this.pool = pool;
        this.user = user;
        this.match = match;
        this.pointsAwarded = pointsAwarded;
        this.ruleVersion = ruleVersion;
        this.resultChecksum = resultChecksum;
        this.updatedAtScore = updatedAtScore;
    }

    public PredictionCurrentScore(Prediction prediction,
                                  PredictionPool pool,
                                  ManagedParticipant managedParticipant,
                                  Match match,
                                  int pointsAwarded,
                                  int ruleVersion,
                                  String resultChecksum,
                                  Instant updatedAtScore) {
        this.prediction = prediction;
        this.pool = pool;
        this.managedParticipant = managedParticipant;
        this.match = match;
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

    public Prediction getPrediction() {
        return prediction;
    }

    public PredictionPool getPool() {
        return pool;
    }

    public UserAccount getUser() {
        return user;
    }

    public ManagedParticipant getManagedParticipant() {
        return managedParticipant;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }
}
