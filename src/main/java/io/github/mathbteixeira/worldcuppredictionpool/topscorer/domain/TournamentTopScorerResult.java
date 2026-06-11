package io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Player;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "tournament_top_scorer_results", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tournament_top_scorer_result", columnNames = {"tournament_id"})
})
public class TournamentTopScorerResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false, unique = true)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private int goals;

    @Column(nullable = false)
    private boolean confirmed;

    @Column(nullable = false)
    private Instant finalizedAt;

    @Column(nullable = false, length = 120)
    private String resultChecksum;

    protected TournamentTopScorerResult() {
    }

    public TournamentTopScorerResult(Tournament tournament, Player player, int goals, boolean confirmed, Instant finalizedAt, String resultChecksum) {
        this.tournament = tournament;
        this.player = player;
        this.goals = goals;
        this.confirmed = confirmed;
        this.finalizedAt = finalizedAt;
        this.resultChecksum = resultChecksum;
    }

    public void updateResult(Player player, int goals, boolean confirmed, Instant finalizedAt, String resultChecksum) {
        this.player = player;
        this.goals = goals;
        this.confirmed = confirmed;
        this.finalizedAt = finalizedAt;
        this.resultChecksum = resultChecksum;
    }

    public Player getPlayer() {
        return player;
    }

    public int getGoals() {
        return goals;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
