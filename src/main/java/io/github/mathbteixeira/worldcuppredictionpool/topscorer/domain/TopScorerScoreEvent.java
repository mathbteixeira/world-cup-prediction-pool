package io.github.mathbteixeira.worldcuppredictionpool.topscorer.domain;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "top_scorer_score_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_top_scorer_score_prediction_checksum", columnNames = {"prediction_id", "result_checksum"})
})
public class TopScorerScoreEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false)
    private PredictionPool pool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_id", nullable = false)
    private TopScorerPrediction prediction;

    @Column(nullable = false)
    private int pointsAwarded;

    @Column(nullable = false)
    private int playerPointsAwarded;

    @Column(nullable = false)
    private int goalsPointsAwarded;

    @Column(nullable = false, length = 500)
    private String explanation;

    @Column(nullable = false)
    private int ruleVersion;

    @Column(nullable = false, length = 120)
    private String resultChecksum;

    @Column(nullable = false)
    private Instant calculatedAt;
}
