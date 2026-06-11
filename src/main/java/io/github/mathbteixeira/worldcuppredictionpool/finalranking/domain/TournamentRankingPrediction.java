package io.github.mathbteixeira.worldcuppredictionpool.finalranking.domain;

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
import java.util.List;
import java.util.UUID;

/**
 * A registered user's prediction of the four teams that will finish the
 * tournament in 1st (champion), 2nd (runner-up), 3rd and 4th place, scoped to a
 * single tournament pool.
 */
@Entity
@Table(name = "tournament_ranking_predictions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tournament_ranking_prediction", columnNames = {"pool_id", "user_id"})
})
public class TournamentRankingPrediction extends BaseEntity {

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
    @JoinColumn(name = "champion_team_id", nullable = false)
    private Team championTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "runner_up_team_id", nullable = false)
    private Team runnerUpTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "third_place_team_id", nullable = false)
    private Team thirdPlaceTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fourth_place_team_id", nullable = false)
    private Team fourthPlaceTeam;

    @Column(nullable = false)
    private Instant submittedAt;

    protected TournamentRankingPrediction() {
    }

    public TournamentRankingPrediction(PredictionPool pool,
                                       UserAccount user,
                                       Tournament tournament,
                                       Team championTeam,
                                       Team runnerUpTeam,
                                       Team thirdPlaceTeam,
                                       Team fourthPlaceTeam,
                                       Instant submittedAt) {
        this.pool = pool;
        this.user = user;
        this.tournament = tournament;
        this.championTeam = championTeam;
        this.runnerUpTeam = runnerUpTeam;
        this.thirdPlaceTeam = thirdPlaceTeam;
        this.fourthPlaceTeam = fourthPlaceTeam;
        this.submittedAt = submittedAt;
    }

    public void resubmit(Team championTeam,
                         Team runnerUpTeam,
                         Team thirdPlaceTeam,
                         Team fourthPlaceTeam,
                         Instant submittedAt) {
        this.championTeam = championTeam;
        this.runnerUpTeam = runnerUpTeam;
        this.thirdPlaceTeam = thirdPlaceTeam;
        this.fourthPlaceTeam = fourthPlaceTeam;
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

    public Team getChampionTeam() {
        return championTeam;
    }

    public Team getRunnerUpTeam() {
        return runnerUpTeam;
    }

    public Team getThirdPlaceTeam() {
        return thirdPlaceTeam;
    }

    public Team getFourthPlaceTeam() {
        return fourthPlaceTeam;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    /** Predicted podium team ids ordered 1st..4th. */
    public List<UUID> orderedTeamIds() {
        return List.of(
                championTeam.getId(),
                runnerUpTeam.getId(),
                thirdPlaceTeam.getId(),
                fourthPlaceTeam.getId()
        );
    }
}
