package io.github.mathbteixeira.worldcuppredictionpool.scoring;

import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolRole;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence.PredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.MatchResultScoringService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.RecalculationResult;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.UpsertMatchResultCommand;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.LeaderboardEntryRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.ScoreEventRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
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
    private PredictionRepository predictionRepository;

    @Autowired
    private ScoreEventRepository scoreEventRepository;

    @Autowired
    private LeaderboardEntryRepository leaderboardEntryRepository;

    private UUID matchId;
    private UUID poolId;

    @BeforeEach
    void setUp() {
        leaderboardEntryRepository.deleteAll();
        scoreEventRepository.deleteAll();
        predictionRepository.deleteAll();
        poolMembershipRepository.deleteAll();
        predictionPoolRepository.deleteAll();
        matchRepository.deleteAll();
        teamRepository.deleteAll();
        tournamentRepository.deleteAll();
        userAccountRepository.deleteAll();

        UserAccount owner = userAccountRepository.save(new UserAccount("owner", "owner@example.com", "hash", UserRole.USER));
        UserAccount guest = userAccountRepository.save(new UserAccount("guest", "guest@example.com", "hash", UserRole.USER));

        Tournament tournament = tournamentRepository.save(new Tournament("World Cup", "world-cup-2026", 2026, TournamentStatus.OPEN));
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

        PredictionPool pool = predictionPoolRepository.save(new PredictionPool("Office", "A", "INVITE01", owner));
        poolMembershipRepository.save(new PoolMembership(pool, owner, PoolRole.OWNER));
        poolMembershipRepository.save(new PoolMembership(pool, guest, PoolRole.MEMBER));

        predictionRepository.save(new Prediction(pool, match, owner, 1, 1, Instant.parse("2026-06-10T10:00:00Z")));
        predictionRepository.save(new Prediction(pool, match, guest, 1, 0, Instant.parse("2026-06-10T10:05:00Z")));

        this.matchId = match.getId();
        this.poolId = pool.getId();
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
}
