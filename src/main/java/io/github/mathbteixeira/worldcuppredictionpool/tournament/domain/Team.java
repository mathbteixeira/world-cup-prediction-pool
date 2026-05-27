package io.github.mathbteixeira.worldcuppredictionpool.tournament.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "teams")
public class Team extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 3)
    private String fifaCode;

    protected Team() {
    }

    public Team(Tournament tournament, String name, String fifaCode) {
        this.tournament = tournament;
        this.name = name;
        this.fifaCode = fifaCode;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public String getName() {
        return name;
    }

    public String getFifaCode() {
        return fifaCode;
    }
}
