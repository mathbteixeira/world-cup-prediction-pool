package io.github.mathbteixeira.worldcuppredictionpool.pool.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "managed_participants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_managed_participant_pool_name", columnNames = {"pool_id", "display_name"})
})
public class ManagedParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private PredictionPool pool;

    @Column(nullable = false, length = 80)
    private String displayName;

    protected ManagedParticipant() {
    }

    public ManagedParticipant(PredictionPool pool, String displayName) {
        this.pool = pool;
        this.displayName = displayName;
    }

    public PredictionPool getPool() {
        return pool;
    }

    public String getDisplayName() {
        return displayName;
    }
}
