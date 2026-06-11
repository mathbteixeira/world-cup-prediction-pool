package io.github.mathbteixeira.worldcuppredictionpool.scoring;

import io.github.mathbteixeira.worldcuppredictionpool.finalranking.api.TournamentRankingPicks;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.application.TournamentRankingPredictionService;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.application.TournamentRankingScoringService;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence.TournamentOfficialRankingRepository;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence.TournamentRankingCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence.TournamentRankingPredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.persistence.TournamentRankingScoreEventRepository;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.application.GroupStandingPredictionService;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.application.GroupStandingScoringService;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence.GroupOfficialStandingRepository;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence.GroupStandingCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence.GroupStandingPredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence.GroupStandingScoreEventRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolRole;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.StandingsRecalculationResult;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.LeaderboardEntryRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StandingsAndRankingScoringIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.8");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private TournamentRepository tournamentRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private PredictionPoolRepository predictionPoolRepository;
    @Autowired private PoolMembershipRepository poolMembershipRepository;
    @Autowired private LeaderboardEntryRepository leaderboardEntryRepository;

    @Autowired private GroupStandingPredictionService groupStandingPredictionService;
    @Autowired private GroupStandingScoringService groupStandingScoringService;
    @Autowired private GroupStandingPredictionRepository groupStandingPredictionRepository;
    @Autowired private GroupStandingScoreEventRepository groupStandingScoreEventRepository;
    @Autowired private GroupStandingCurrentScoreRepository groupStandingCurrentScoreRepository;
    @Autowired private GroupOfficialStandingRepository groupOfficialStandingRepository;

    @Autowired private TournamentRankingPredictionService tournamentRankingPredictionService;
    @Autowired private TournamentRankingScoringService tournamentRankingScoringService;
    @Autowired private TournamentRankingPredictionRepository tournamentRankingPredictionRepository;
    @Autowired private TournamentRankingScoreEventRepository tournamentRankingScoreEventRepository;
    @Autowired private TournamentRankingCurrentScoreRepository tournamentRankingCurrentScoreRepository;
    @Autowired private TournamentOfficialRankingRepository tournamentOfficialRankingRepository;

    private UUID tournamentId;
    private UUID poolId;
    private List<UUID> groupOrder;

    @BeforeEach
    void setUp() {
        leaderboardEntryRepository.deleteAllInBatch();
        groupStandingScoreEventRepository.deleteAllInBatch();
        groupStandingCurrentScoreRepository.deleteAllInBatch();
        groupStandingPredictionRepository.deleteAllInBatch();
        groupOfficialStandingRepository.deleteAllInBatch();
        tournamentRankingScoreEventRepository.deleteAllInBatch();
        tournamentRankingCurrentScoreRepository.deleteAllInBatch();
        tournamentRankingPredictionRepository.deleteAllInBatch();
        tournamentOfficialRankingRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        poolMembershipRepository.deleteAllInBatch();
        predictionPoolRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();
        tournamentRepository.deleteAllInBatch();
        userAccountRepository.deleteAllInBatch();

        UserAccount owner = userAccountRepository.save(new UserAccount("owner", "owner@example.com", "hash", UserRole.USER));
        UserAccount guest = userAccountRepository.save(new UserAccount("guest", "guest@example.com", "hash", UserRole.USER));

        Tournament tournament = tournamentRepository.save(new Tournament("World Cup", "world-cup-2026", "2026", TournamentStatus.OPEN));
        Team a = teamRepository.save(new Team(tournament, "Alpha", "ALP"));
        Team b = teamRepository.save(new Team(tournament, "Bravo", "BRV"));
        Team c = teamRepository.save(new Team(tournament, "Charlie", "CHA"));
        Team d = teamRepository.save(new Team(tournament, "Delta", "DEL"));
        // Two group-stage matches so the catalog derives the four group-A teams.
        matchRepository.save(new Match(tournament, a, b, Instant.parse("2100-06-10T20:00:00Z"), "GROUP_STAGE", "A", MatchStatus.SCHEDULED));
        matchRepository.save(new Match(tournament, c, d, Instant.parse("2100-06-10T23:00:00Z"), "GROUP_STAGE", "A", MatchStatus.SCHEDULED));

        PredictionPool pool = predictionPoolRepository.save(new PredictionPool("Office", "A", "INVITE01", owner, tournament));
        poolMembershipRepository.save(new PoolMembership(pool, owner, PoolRole.OWNER));
        poolMembershipRepository.save(new PoolMembership(pool, guest, PoolRole.MEMBER));

        this.tournamentId = tournament.getId();
        this.poolId = pool.getId();
        this.groupOrder = List.of(a.getId(), b.getId(), c.getId(), d.getId());

        // owner predicts the exact order; guest swaps the first two.
        groupStandingPredictionService.submit(poolId, "A", "owner@example.com", groupOrder);
        groupStandingPredictionService.submit(poolId, "A", "guest@example.com",
                List.of(b.getId(), a.getId(), c.getId(), d.getId()));
    }

    private Map<String, Integer> leaderboardTotals() {
        return leaderboardEntryRepository.findAllByPoolIdOrderByRankPositionAsc(poolId).stream()
                .collect(Collectors.toMap(e -> e.getParticipantName(), e -> e.getTotalPoints()));
    }

    @Test
    void shouldScoreGroupStandingsAndRebuildLeaderboard() {
        StandingsRecalculationResult result = groupStandingScoringService.confirmAndRecalculate(tournamentId, "A", groupOrder);

        assertThat(result.scoredPredictions()).isEqualTo(2);
        assertThat(result.affectedPools()).isEqualTo(1);
        assertThat(result.idempotentReplay()).isFalse();

        Map<String, Integer> totals = leaderboardTotals();
        assertThat(totals.get("owner")).isEqualTo(40); // 4 correct * 10
        assertThat(totals.get("guest")).isEqualTo(20); // 2 correct * 10

        // Replaying the same official standings is idempotent.
        StandingsRecalculationResult replay = groupStandingScoringService.confirmAndRecalculate(tournamentId, "A", groupOrder);
        assertThat(replay.idempotentReplay()).isTrue();
        assertThat(groupStandingScoreEventRepository.count()).isEqualTo(2);
        assertThat(leaderboardTotals().get("owner")).isEqualTo(40);
    }

    @Test
    void shouldCombineGroupAndFinalRankingPointsInOneLeaderboard() {
        groupStandingScoringService.confirmAndRecalculate(tournamentId, "A", groupOrder);

        UUID a = groupOrder.get(0);
        UUID b = groupOrder.get(1);
        UUID c = groupOrder.get(2);
        UUID d = groupOrder.get(3);

        // owner's podium is fully correct; guest only gets the champion right.
        tournamentRankingPredictionService.submit(poolId, "owner@example.com", new TournamentRankingPicks(a, b, c, d));
        tournamentRankingPredictionService.submit(poolId, "guest@example.com", new TournamentRankingPicks(a, d, b, c));

        StandingsRecalculationResult result = tournamentRankingScoringService.confirmAndRecalculate(tournamentId, a, b, c, d);
        assertThat(result.scoredPredictions()).isEqualTo(2);

        Map<String, Integer> totals = leaderboardTotals();
        assertThat(totals.get("owner")).isEqualTo(40 + 68); // group 40 + podium 20+18+15+15
        assertThat(totals.get("guest")).isEqualTo(20 + 20); // group 20 + champion 20
    }
}