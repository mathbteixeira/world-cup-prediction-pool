package io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
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
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 120)
    private String playerName;

    @Column(nullable = false)
    private int predictedGoals;

    @Column(nullable = false)
    private Instant submittedAt;

    protected TopScorerPrediction() {
    }

    public TopScorerPrediction(PredictionPool pool, UserAccount user, Tournament tournament, Team team, String playerName, int predictedGoals, Instant submittedAt) {
        this.pool = pool;
        this.user = user;
        this.tournament = tournament;
        this.team = team;
        this.playerName = playerName;
        this.predictedGoals = predictedGoals;
        this.submittedAt = submittedAt;
    }

    public void resubmit(Team team, String playerName, int predictedGoals, Instant submittedAt) {
        this.team = team;
        this.playerName = playerName;
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

    public Team getTeam() {
        return team;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getPredictedGoals() {
        return predictedGoals;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
