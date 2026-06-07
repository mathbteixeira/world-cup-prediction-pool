package io.github.mathbteixeira.worldcuppredictionpool.prediction.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.ManagedParticipant;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "predictions")
public class Prediction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private PredictionPool pool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_participant_id")
    private ManagedParticipant managedParticipant;

    @Column(nullable = false)
    private int predictedHomeScore;

    @Column(nullable = false)
    private int predictedAwayScore;

    @Column(nullable = false)
    private Instant submittedAt;

    protected Prediction() {
    }

    public Prediction(PredictionPool pool,
                      Match match,
                      UserAccount user,
                      int predictedHomeScore,
                      int predictedAwayScore,
                      Instant submittedAt) {
        this.pool = pool;
        this.match = match;
        this.user = user;
        this.predictedHomeScore = predictedHomeScore;
        this.predictedAwayScore = predictedAwayScore;
        this.submittedAt = submittedAt;
    }

    public Prediction(PredictionPool pool,
                      Match match,
                      ManagedParticipant managedParticipant,
                      int predictedHomeScore,
                      int predictedAwayScore,
                      Instant submittedAt) {
        this.pool = pool;
        this.match = match;
        this.managedParticipant = managedParticipant;
        this.predictedHomeScore = predictedHomeScore;
        this.predictedAwayScore = predictedAwayScore;
        this.submittedAt = submittedAt;
    }

    public void resubmit(int predictedHomeScore, int predictedAwayScore, Instant submittedAt) {
        this.predictedHomeScore = predictedHomeScore;
        this.predictedAwayScore = predictedAwayScore;
        this.submittedAt = submittedAt;
    }

    public PredictionPool getPool() {
        return pool;
    }

    public Match getMatch() {
        return match;
    }

    public UserAccount getUser() {
        return user;
    }

    public ManagedParticipant getManagedParticipant() {
        return managedParticipant;
    }

    public boolean isManagedParticipantPrediction() {
        return managedParticipant != null;
    }

    public int getPredictedHomeScore() {
        return predictedHomeScore;
    }

    public int getPredictedAwayScore() {
        return predictedAwayScore;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
