package io.github.mathbteixeira.worldcuppredictionpool.groupstage.domain;

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
 * A registered user's predicted final ordering of the four teams in one group,
 * scoped to a single tournament pool. The four positional team references hold
 * the user's guess for 1st through 4th place in the group stage.
 */
@Entity
@Table(name = "group_standing_predictions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_group_standing_prediction", columnNames = {"pool_id", "user_id", "group_name"})
})
public class GroupStandingPrediction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private PredictionPool pool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "group_name", nullable = false, length = 1)
    private String groupName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "first_place_team_id", nullable = false)
    private Team firstPlaceTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "second_place_team_id", nullable = false)
    private Team secondPlaceTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "third_place_team_id", nullable = false)
    private Team thirdPlaceTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fourth_place_team_id", nullable = false)
    private Team fourthPlaceTeam;

    @Column(nullable = false)
    private Instant submittedAt;

    protected GroupStandingPrediction() {
    }

    public GroupStandingPrediction(PredictionPool pool,
                                   UserAccount user,
                                   Tournament tournament,
                                   String groupName,
                                   Team firstPlaceTeam,
                                   Team secondPlaceTeam,
                                   Team thirdPlaceTeam,
                                   Team fourthPlaceTeam,
                                   Instant submittedAt) {
        this.pool = pool;
        this.user = user;
        this.tournament = tournament;
        this.groupName = groupName;
        this.firstPlaceTeam = firstPlaceTeam;
        this.secondPlaceTeam = secondPlaceTeam;
        this.thirdPlaceTeam = thirdPlaceTeam;
        this.fourthPlaceTeam = fourthPlaceTeam;
        this.submittedAt = submittedAt;
    }

    public void resubmit(Team firstPlaceTeam,
                         Team secondPlaceTeam,
                         Team thirdPlaceTeam,
                         Team fourthPlaceTeam,
                         Instant submittedAt) {
        this.firstPlaceTeam = firstPlaceTeam;
        this.secondPlaceTeam = secondPlaceTeam;
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

    public String getGroupName() {
        return groupName;
    }

    public Team getFirstPlaceTeam() {
        return firstPlaceTeam;
    }

    public Team getSecondPlaceTeam() {
        return secondPlaceTeam;
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

    /** The predicted team ids ordered from 1st to 4th place. */
    public List<UUID> orderedTeamIds() {
        return List.of(
                firstPlaceTeam.getId(),
                secondPlaceTeam.getId(),
                thirdPlaceTeam.getId(),
                fourthPlaceTeam.getId()
        );
    }
}