package io.github.mathbteixeira.worldcuppredictionpool.scoring.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.ManagedParticipant;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leaderboard_entries")
public class LeaderboardEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private PredictionPool pool;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_participant_id")
    private ManagedParticipant managedParticipant;

    @Column(nullable = false)
    private int totalPoints;

    @Column(nullable = false)
    private int rankPosition;

    @Column(nullable = false)
    private Instant recalculatedAt;

    protected LeaderboardEntry() {
    }

    public LeaderboardEntry(PredictionPool pool, UserAccount user, int totalPoints, int rankPosition, Instant recalculatedAt) {
        this.pool = pool;
        this.user = user;
        this.totalPoints = totalPoints;
        this.rankPosition = rankPosition;
        this.recalculatedAt = recalculatedAt;
    }

    public LeaderboardEntry(PredictionPool pool, ManagedParticipant managedParticipant, int totalPoints, int rankPosition, Instant recalculatedAt) {
        this.pool = pool;
        this.managedParticipant = managedParticipant;
        this.totalPoints = totalPoints;
        this.rankPosition = rankPosition;
        this.recalculatedAt = recalculatedAt;
    }

    public PredictionPool getPool() {
        return pool;
    }

    public UserAccount getUser() {
        return user;
    }

    public ManagedParticipant getManagedParticipant() {
        return managedParticipant;
    }

    public UUID getParticipantId() {
        return user == null ? managedParticipant.getId() : user.getId();
    }

    public String getParticipantName() {
        return user == null ? managedParticipant.getDisplayName() : user.getUsername();
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public int getRankPosition() {
        return rankPosition;
    }

    public Instant getRecalculatedAt() {
        return recalculatedAt;
    }
}
