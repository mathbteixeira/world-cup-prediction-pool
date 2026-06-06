package io.github.mathbteixeira.worldcuppredictionpool.scoring;

import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.ManagedParticipant;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolRole;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.ManagedParticipantRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence.PredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.MatchResultScoringService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.RecalculationResult;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.UpsertMatchResultCommand;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.LeaderboardEntryRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.PredictionCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.ScoreEventRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchResultRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TeamRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TournamentRepository;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserRole;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MatchResultScoringServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.8");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MatchResultScoringService matchResultScoringService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PredictionPoolRepository predictionPoolRepository;

    @Autowired
    private PoolMembershipRepository poolMembershipRepository;

    @Autowired
    private ManagedParticipantRepository managedParticipantRepository;

    @Autowired
    private PredictionRepository predictionRepository;

    @Autowired
    private ScoreEventRepository scoreEventRepository;

    @Autowired
    private PredictionCurrentScoreRepository predictionCurrentScoreRepository;

    @Autowired
    private LeaderboardEntryRepository leaderboardEntryRepository;

    @Autowired
    private MatchResultRepository matchResultRepository;

    private UUID matchId;
    private UUID poolId;

    @BeforeEach
    void setUp() {
        leaderboardEntryRepository.deleteAllInBatch();
        scoreEventRepository.deleteAllInBatch();
        predictionCurrentScoreRepository.deleteAllInBatch();
        matchResultRepository.deleteAllInBatch();
        predictionRepository.deleteAllInBatch();
        managedParticipantRepository.deleteAllInBatch();
        poolMembershipRepository.deleteAllInBatch();
        predictionPoolRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();
        tournamentRepository.deleteAllInBatch();
        userAccountRepository.deleteAllInBatch();

        UserAccount owner = userAccountRepository.save(new UserAccount("owner", "owner@example.com", "hash", UserRole.USER));
        UserAccount guest = userAccountRepository.save(new UserAccount("guest", "guest@example.com", "hash", UserRole.USER));

        Tournament tournament = tournamentRepository.save(new Tournament("World Cup", "world-cup-2026", "2026", TournamentStatus.OPEN));
        Team home = teamRepository.save(new Team(tournament, "Brazil", "BRA"));
        Team away = teamRepository.save(new Team(tournament, "Spain", "ESP"));
        Match match = matchRepository.save(new Match(
                tournament,
                home,
                away,
                Instant.parse("2026-06-10T20:00:00Z"),
                "GROUP",
                MatchStatus.SCHEDULED
        ));

        PredictionPool pool = predictionPoolRepository.save(new PredictionPool("Office", "A", "INVITE01", owner, tournament));
        poolMembershipRepository.save(new PoolMembership(pool, owner, PoolRole.OWNER));
        poolMembershipRepository.save(new PoolMembership(pool, guest, PoolRole.MEMBER));

        predictionRepository.save(new Prediction(pool, match, owner, 1, 1, Instant.parse("2026-06-10T10:00:00Z")));
        predictionRepository.save(new Prediction(pool, match, guest, 1, 0, Instant.parse("2026-06-10T10:05:00Z")));

        this.matchId = match.getId();
        this.poolId = pool.getId();
    }


    @Test
    @Transactional
    void shouldIgnoreDuplicateScoreEventInsertOnConflict() {
        Prediction prediction = predictionRepository.findAll().get(0);
        Instant now = Instant.parse("2026-06-10T21:00:00Z");
        String checksum = "race-checksum";

        int firstInsert = scoreEventRepository.insertIgnoreConflict(
                UUID.randomUUID(),
                now,
                now,
                prediction.getPool().getId(),
                prediction.getUser().getId(),
                null,
                prediction.getMatch().getId(),
                prediction.getId(),
                5,
                5,
                0,
                0,
                "exact score",
                1,
                checksum,
                now
        );

        int secondInsert = scoreEventRepository.insertIgnoreConflict(
                UUID.randomUUID(),
                now,
                now,
                prediction.getPool().getId(),
                prediction.getUser().getId(),
                null,
                prediction.getMatch().getId(),
                prediction.getId(),
                3,
                0,
                3,
                0,
                "outcome",
                1,
                checksum,
                now
        );

        assertThat(firstInsert).isEqualTo(1);
        assertThat(secondInsert).isEqualTo(0);
        assertThat(scoreEventRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldBeIdempotentAndRecalculateWhenResultChanges() {
        RecalculationResult first = matchResultScoringService.upsertResultAndRecalculate(
                new UpsertMatchResultCommand(matchId, 2, 1, null, null, true)
        );

        assertThat(first.idempotentReplay()).isFalse();
        assertThat(first.scoredPredictions()).isEqualTo(2);
        assertThat(scoreEventRepository.count()).isEqualTo(2);

        var firstLeaderboard = leaderboardEntryRepository.findAllByPoolIdOrderByRankPositionAsc(poolId);
        assertThat(firstLeaderboard).hasSize(2);
        assertThat(firstLeaderboard.get(0).getTotalPoints()).isEqualTo(5);
        assertThat(firstLeaderboard.get(1).getTotalPoints()).isEqualTo(0);

        RecalculationResult replay = matchResultScoringService.upsertResultAndRecalculate(
                new UpsertMatchResultCommand(matchId, 2, 1, null, null, true)
        );

        assertThat(replay.idempotentReplay()).isTrue();
        assertThat(scoreEventRepository.count()).isEqualTo(2);

        RecalculationResult changed = matchResultScoringService.upsertResultAndRecalculate(
                new UpsertMatchResultCommand(matchId, 2, 0, null, null, true)
        );

        assertThat(changed.idempotentReplay()).isFalse();
        assertThat(scoreEventRepository.count()).isEqualTo(4);

        var changedLeaderboard = leaderboardEntryRepository.findAllByPoolIdOrderByRankPositionAsc(poolId);
        assertThat(changedLeaderboard).hasSize(2);
        assertThat(changedLeaderboard.get(0).getTotalPoints()).isEqualTo(3);
        assertThat(changedLeaderboard.get(1).getTotalPoints()).isEqualTo(0);
    }

    @Test
    void shouldScoreSingleMatchPoolAndRebuildItsLeaderboard() {
        Match match = matchRepository.findById(matchId).orElseThrow();
        UserAccount owner = userAccountRepository.findByEmailIgnoreCase("owner@example.com").orElseThrow();
        UserAccount guest = userAccountRepository.findByEmailIgnoreCase("guest@example.com").orElseThrow();

        PredictionPool singleMatchPool = predictionPoolRepository.save(new PredictionPool(
                "Family Friendly",
                "One match",
                "INVITE02",
                owner,
                match
        ));
        poolMembershipRepository.save(new PoolMembership(singleMatchPool, owner, PoolRole.OWNER));
        poolMembershipRepository.save(new PoolMembership(singleMatchPool, guest, PoolRole.MEMBER));
        predictionRepository.save(new Prediction(singleMatchPool, match, owner, 2, 1, Instant.parse("2026-06-10T10:10:00Z")));
        predictionRepository.save(new Prediction(singleMatchPool, match, guest, 0, 0, Instant.parse("2026-06-10T10:15:00Z")));

        RecalculationResult result = matchResultScoringService.upsertResultAndRecalculate(
                new UpsertMatchResultCommand(matchId, 2, 1, null, null, true)
        );

        assertThat(result.affectedPools()).isEqualTo(2);
        var leaderboard = leaderboardEntryRepository.findAllByPoolIdOrderByRankPositionAsc(singleMatchPool.getId());
        assertThat(leaderboard).hasSize(2);
        assertThat(leaderboard.get(0).getTotalPoints()).isEqualTo(5);
        assertThat(leaderboard.get(1).getTotalPoints()).isEqualTo(0);
    }

    @Test
    void shouldIncludeManagedParticipantsInLeaderboardAfterRecalculation() {
        Match match = matchRepository.findById(matchId).orElseThrow();
        UserAccount owner = userAccountRepository.findByEmailIgnoreCase("owner@example.com").orElseThrow();

        PredictionPool singleMatchPool = predictionPoolRepository.save(new PredictionPool(
                "Family Friendly",
                "One match",
                "INVITE03",
                owner,
                match
        ));
        poolMembershipRepository.save(new PoolMembership(singleMatchPool, owner, PoolRole.OWNER));
        ManagedParticipant grandma = managedParticipantRepository.save(new ManagedParticipant(singleMatchPool, "Grandma"));
        predictionRepository.save(new Prediction(singleMatchPool, match, grandma, 2, 1, Instant.parse("2026-06-10T10:20:00Z")));

        matchResultScoringService.upsertResultAndRecalculate(
                new UpsertMatchResultCommand(matchId, 2, 1, null, null, true)
        );

        var leaderboard = leaderboardEntryRepository.findAllByPoolIdOrderByRankPositionAsc(singleMatchPool.getId());
        assertThat(leaderboard).hasSize(2);
        assertThat(leaderboard.get(0).getManagedParticipant().getDisplayName()).isEqualTo("Grandma");
        assertThat(leaderboard.get(0).getTotalPoints()).isEqualTo(5);
    }
}
