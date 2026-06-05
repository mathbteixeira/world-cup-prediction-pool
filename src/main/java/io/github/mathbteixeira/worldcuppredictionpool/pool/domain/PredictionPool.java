package io.github.mathbteixeira.worldcuppredictionpool.pool.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "prediction_pools")
public class PredictionPool extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, unique = true, length = 20)
    private String inviteCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PoolScope poolScope = PoolScope.TOURNAMENT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "single_match_id")
    private Match singleMatch;

    protected PredictionPool() {
    }

    public PredictionPool(String name, String description, String inviteCode, UserAccount owner, Tournament tournament) {
        this.name = name;
        this.description = description;
        this.inviteCode = inviteCode;
        this.owner = owner;
        this.tournament = tournament;
    }

    public PredictionPool(String name, String description, String inviteCode, UserAccount owner, Match singleMatch) {
        this.name = name;
        this.description = description;
        this.inviteCode = inviteCode;
        this.poolScope = PoolScope.SINGLE_MATCH;
        this.owner = owner;
        this.tournament = singleMatch.getTournament();
        this.singleMatch = singleMatch;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public PoolScope getPoolScope() {
        return poolScope;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public Match getSingleMatch() {
        return singleMatch;
    }

    public boolean isSingleMatchPool() {
        return poolScope == PoolScope.SINGLE_MATCH;
    }
}
