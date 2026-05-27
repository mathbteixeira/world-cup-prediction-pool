package io.github.mathbteixeira.worldcuppredictionpool.pool.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
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
@Table(name = "pool_memberships")
public class PoolMembership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private PredictionPool pool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PoolRole role;

    protected PoolMembership() {
    }

    public PoolMembership(PredictionPool pool, UserAccount user, PoolRole role) {
        this.pool = pool;
        this.user = user;
        this.role = role;
    }

    public PredictionPool getPool() {
        return pool;
    }

    public UserAccount getUser() {
        return user;
    }

    public PoolRole getRole() {
        return role;
    }
}
