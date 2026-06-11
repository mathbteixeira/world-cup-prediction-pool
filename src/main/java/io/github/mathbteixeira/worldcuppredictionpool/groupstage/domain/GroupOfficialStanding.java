package io.github.mathbteixeira.worldcuppredictionpool.groupstage.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
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
 * The official, admin-confirmed final ordering of the four teams in a group.
 * This is the source of truth that group-position predictions are scored
 * against. The {@code resultChecksum} captures the confirmed ordering so that
 * re-confirming the same standings is an idempotent no-op, while confirming a
 * corrected ordering produces a fresh set of score events.
 */
@Entity
@Table(name = "group_official_standings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_group_official_standing", columnNames = {"tournament_id", "group_name"})
})
public class GroupOfficialStanding extends BaseEntity {

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
    private boolean confirmed;

    @Column(nullable = false)
    private Instant finalizedAt;

    @Column(nullable = false, length = 120)
    private String resultChecksum;

    protected GroupOfficialStanding() {
    }

    public GroupOfficialStanding(Tournament tournament,
                                 String groupName,
                                 Team firstPlaceTeam,
                                 Team secondPlaceTeam,
                                 Team thirdPlaceTeam,
                                 Team fourthPlaceTeam,
                                 boolean confirmed,
                                 Instant finalizedAt,
                                 String resultChecksum) {
        this.tournament = tournament;
        this.groupName = groupName;
        this.firstPlaceTeam = firstPlaceTeam;
        this.secondPlaceTeam = secondPlaceTeam;
        this.thirdPlaceTeam = thirdPlaceTeam;
        this.fourthPlaceTeam = fourthPlaceTeam;
        this.confirmed = confirmed;
        this.finalizedAt = finalizedAt;
        this.resultChecksum = resultChecksum;
    }

    public void updateStanding(Team firstPlaceTeam,
                               Team secondPlaceTeam,
                               Team thirdPlaceTeam,
                               Team fourthPlaceTeam,
                               boolean confirmed,
                               Instant finalizedAt,
                               String resultChecksum) {
        this.firstPlaceTeam = firstPlaceTeam;
        this.secondPlaceTeam = secondPlaceTeam;
        this.thirdPlaceTeam = thirdPlaceTeam;
        this.fourthPlaceTeam = fourthPlaceTeam;
        this.confirmed = confirmed;
        this.finalizedAt = finalizedAt;
        this.resultChecksum = resultChecksum;
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

    public boolean isConfirmed() {
        return confirmed;
    }

    public Instant getFinalizedAt() {
        return finalizedAt;
    }

    public String getResultChecksum() {
        return resultChecksum;
    }

    /** The official team ids ordered from 1st to 4th place. */
    public List<UUID> orderedTeamIds() {
        return List.of(
                firstPlaceTeam.getId(),
                secondPlaceTeam.getId(),
                thirdPlaceTeam.getId(),
                fourthPlaceTeam.getId()
        );
    }
}