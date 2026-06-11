package io.github.mathbteixeira.worldcuppredictionpool.finalranking.domain;

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
 * The official, admin-confirmed final top-four ranking of a tournament. This is
 * the source of truth that podium predictions are scored against. The
 * {@code resultChecksum} makes re-confirming the same ranking idempotent.
 */
@Entity
@Table(name = "tournament_official_rankings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tournament_official_ranking", columnNames = {"tournament_id"})
})
public class TournamentOfficialRanking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false, unique = true)
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
    private boolean confirmed;

    @Column(nullable = false)
    private Instant finalizedAt;

    @Column(nullable = false, length = 120)
    private String resultChecksum;

    protected TournamentOfficialRanking() {
    }

    public TournamentOfficialRanking(Tournament tournament,
                                     Team championTeam,
                                     Team runnerUpTeam,
                                     Team thirdPlaceTeam,
                                     Team fourthPlaceTeam,
                                     boolean confirmed,
                                     Instant finalizedAt,
                                     String resultChecksum) {
        this.tournament = tournament;
        this.championTeam = championTeam;
        this.runnerUpTeam = runnerUpTeam;
        this.thirdPlaceTeam = thirdPlaceTeam;
        this.fourthPlaceTeam = fourthPlaceTeam;
        this.confirmed = confirmed;
        this.finalizedAt = finalizedAt;
        this.resultChecksum = resultChecksum;
    }

    public void updateRanking(Team championTeam,
                              Team runnerUpTeam,
                              Team thirdPlaceTeam,
                              Team fourthPlaceTeam,
                              boolean confirmed,
                              Instant finalizedAt,
                              String resultChecksum) {
        this.championTeam = championTeam;
        this.runnerUpTeam = runnerUpTeam;
        this.thirdPlaceTeam = thirdPlaceTeam;
        this.fourthPlaceTeam = fourthPlaceTeam;
        this.confirmed = confirmed;
        this.finalizedAt = finalizedAt;
        this.resultChecksum = resultChecksum;
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

    public boolean isConfirmed() {
        return confirmed;
    }

    public Instant getFinalizedAt() {
        return finalizedAt;
    }

    public String getResultChecksum() {
        return resultChecksum;
    }

    /** Official podium team ids ordered 1st..4th. */
    public List<UUID> orderedTeamIds() {
        return List.of(
                championTeam.getId(),
                runnerUpTeam.getId(),
                thirdPlaceTeam.getId(),
                fourthPlaceTeam.getId()
        );
    }
}