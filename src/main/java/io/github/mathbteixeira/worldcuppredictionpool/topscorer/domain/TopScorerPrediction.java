package io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Player;
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

@Entity
@Table(name = "top_scorer_predictions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_top_scorer_prediction", columnNames = {"pool_id", "user_id"})
})
public class TopScorerPrediction extends BaseEntity {

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
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private int predictedGoals;

    @Column(nullable = false)
    private Instant submittedAt;

    protected TopScorerPrediction() {
    }

    public TopScorerPrediction(PredictionPool pool, UserAccount user, Tournament tournament, Player player, int predictedGoals, Instant submittedAt) {
        this.pool = pool;
        this.user = user;
        this.tournament = tournament;
        this.player = player;
        this.predictedGoals = predictedGoals;
        this.submittedAt = submittedAt;
    }

    public void resubmit(Player player, int predictedGoals, Instant submittedAt) {
        this.player = player;
        this.predictedGoals = predictedGoals;
        this.submittedAt = submittedAt;
    }

    public PredictionPool getPool() {
        return pool;
    }

    public UserAccount getUser() {
        return user;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public Player getPlayer() {
        return player;
    }

    public int getPredictedGoals() {
        return predictedGoals;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
