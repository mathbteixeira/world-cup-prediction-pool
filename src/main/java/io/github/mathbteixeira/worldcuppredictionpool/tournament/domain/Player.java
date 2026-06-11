package io.github.mathbteixeira.worldcuppredictionpool.tournament.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "players", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_team_name", columnNames = {"team_id", "name"})
})
public class Player extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int rosterNumber;

    protected Player() {
    }

    public Player(Team team, String name, int rosterNumber) {
        this.team = team;
        this.name = name;
        this.rosterNumber = rosterNumber;
    }

    public Team getTeam() {
        return team;
    }

    public String getName() {
        return name;
    }

    public int getRosterNumber() {
        return rosterNumber;
    }
}
