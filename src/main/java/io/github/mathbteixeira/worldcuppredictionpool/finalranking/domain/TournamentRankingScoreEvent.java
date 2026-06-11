package io.github.mathbteixeira.worldcuppredictionpool.finalranking.domain;

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

/**
 * Immutable, append-only audit record of the points a podium prediction earned
 * for one confirmed final-ranking snapshot, with a per-position breakdown.
 * Unique on {@code (prediction_id, result_checksum)} for idempotent replays.
 */
@Entity
@Table(name = "tournament_ranking_score_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tournament_ranking_score_event", columnNames = {"prediction_id", "result_checksum"})
})
public class TournamentRankingScoreEvent extends BaseEntity {

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
    private TournamentRankingPrediction prediction;

    @Column(nullable = false)
    private int pointsAwarded;

    @Column(nullable = false)
    private int championPointsAwarded;

    @Column(nullable = false)
    private int runnerUpPointsAwarded;

    @Column(nullable = false)
    private int thirdPlacePointsAwarded;

    @Column(nullable = false)
    private int fourthPlacePointsAwarded;

    @Column(nullable = false, length = 255)
    private String explanation;

    @Column(nullable = false)
    private int ruleVersion;

    @Column(nullable = false, length = 120)
    private String resultChecksum;

    @Column(nullable = false)
    private Instant calculatedAt;

    protected TournamentRankingScoreEvent() {
    }

    public TournamentRankingScoreEvent(PredictionPool pool,
                                       UserAccount user,
                                       Tournament tournament,
                                       TournamentRankingPrediction prediction,
                                       int pointsAwarded,
                                       int championPointsAwarded,
                                       int runnerUpPointsAwarded,
                                       int thirdPlacePointsAwarded,
                                       int fourthPlacePointsAwarded,
                                       String explanation,
                                       int ruleVersion,
                                       String resultChecksum,
                                       Instant calculatedAt) {
        this.pool = pool;
        this.user = user;
        this.tournament = tournament;
        this.prediction = prediction;
        this.pointsAwarded = pointsAwarded;
        this.championPointsAwarded = championPointsAwarded;
        this.runnerUpPointsAwarded = runnerUpPointsAwarded;
        this.thirdPlacePointsAwarded = thirdPlacePointsAwarded;
        this.fourthPlacePointsAwarded = fourthPlacePointsAwarded;
        this.explanation = explanation;
        this.ruleVersion = ruleVersion;
        this.resultChecksum = resultChecksum;
        this.calculatedAt = calculatedAt;
    }

    public PredictionPool getPool() {
        return pool;
    }

    public UserAccount getUser() {
        return user;
    }

    public TournamentRankingPrediction getPrediction() {
        return prediction;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }

    public int getRuleVersion() {
        return ruleVersion;
    }

    public String getResultChecksum() {
        return resultChecksum;
    }
}