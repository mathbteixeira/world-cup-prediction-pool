package io.github.mathbteixeira.worldcuppredictionpool.tournament.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "tournaments")
public class Tournament extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false)
    private int seasonYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TournamentStatus status = TournamentStatus.DRAFT;

    protected Tournament() {
    }

    public Tournament(String name, String slug, int seasonYear, TournamentStatus status) {
        this.name = name;
        this.slug = slug;
        this.seasonYear = seasonYear;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public int getSeasonYear() {
        return seasonYear;
    }

    public TournamentStatus getStatus() {
        return status;
    }
}
