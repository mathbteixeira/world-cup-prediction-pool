package io.github.mathbteixeira.worldcuppredictionpool.prediction;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolRole;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.api.PoolPredictionResponse;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.application.PredictionSubmissionService;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.application.SubmitPredictionCommand;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence.PredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchResult;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchResultRepository;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserRole;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictionSubmissionServiceTest {

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private PredictionPoolRepository predictionPoolRepository;

    @Mock
    private PoolMembershipRepository poolMembershipRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    private PredictionSubmissionService predictionSubmissionService;

    @BeforeEach
    void setUp() {
        predictionSubmissionService = new PredictionSubmissionService(
                predictionRepository,
                predictionPoolRepository,
                poolMembershipRepository,
                matchRepository,
                matchResultRepository,
                userAccountRepository,
                Clock.fixed(Instant.parse("2027-06-01T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void shouldListVisiblePoolPredictionsWithMatchDetailsAndResult() {
        UserAccount user = new UserAccount("owner", "owner@example.com", "hash", UserRole.USER);
        Tournament tournament = new Tournament("World Cup", "wc-2026", "2026", TournamentStatus.OPEN);
        setId(user, UUID.randomUUID());
        setId(tournament, UUID.randomUUID());
        PredictionPool pool = new PredictionPool("Pool", "desc", "ABC12345", user, tournament);
        UUID poolId = UUID.randomUUID();
        setId(pool, poolId);
        Team home = new Team(tournament, "Brazil", "BRA");
        Team away = new Team(tournament, "Spain", "ESP");
        setId(home, UUID.randomUUID());
        setId(away, UUID.randomUUID());
        Match match = new Match(
                tournament,
                home,
                away,
                Instant.parse("2027-06-01T11:00:00Z"),
                "GROUP_STAGE",
                "A",
                MatchStatus.FINISHED
        );
        UUID matchId = UUID.randomUUID();
        setId(match, matchId);
        Prediction prediction = new Prediction(pool, match, user, 2, 1, Instant.parse("2027-05-31T10:00:00Z"));
        UUID predictionId = UUID.randomUUID();
        setId(prediction, predictionId);
        MatchResult result = new MatchResult(match, 3, 1, null, null, true, Instant.parse("2027-06-01T15:00:00Z"), "checksum");

        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(userAccountRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, user.getId()))
                .thenReturn(Optional.of(new PoolMembership(pool, user, PoolRole.OWNER)));
        when(predictionRepository.findAllForPoolOrderByKickoffAt(poolId))
                .thenReturn(List.of(prediction));
        when(matchResultRepository.findAllByMatchIdIn(List.of(matchId))).thenReturn(List.of(result));

        List<PoolPredictionResponse> predictions = predictionSubmissionService.listVisiblePoolPredictions(poolId, "owner@example.com");

        assertThat(predictions).hasSize(1);
        PoolPredictionResponse response = predictions.getFirst();
        assertThat(response.predictionId()).isEqualTo(predictionId);
        assertThat(response.poolId()).isEqualTo(poolId);
        assertThat(response.user().userId()).isEqualTo(user.getId());
        assertThat(response.user().username()).isEqualTo("owner");
        assertThat(response.mine()).isTrue();
        assertThat(response.homeScore()).isEqualTo(2);
        assertThat(response.awayScore()).isEqualTo(1);
        assertThat(response.match().matchId()).isEqualTo(matchId);
        assertThat(response.match().homeTeam().fifaCode()).isEqualTo("BRA");
        assertThat(response.match().awayTeam().fifaCode()).isEqualTo("ESP");
        assertThat(response.match().groupName()).isEqualTo("A");
        assertThat(response.match().result().homeScore()).isEqualTo(3);
        assertThat(response.match().predictionOpen()).isFalse();
    }

    @Test
    void shouldHideOtherPoolMembersPredictionsUntilMatchCloses() {
        UserAccount user = new UserAccount("owner", "owner@example.com", "hash", UserRole.USER);
        UserAccount otherUser = new UserAccount("bob", "bob@example.com", "hash", UserRole.USER);
        Tournament tournament = new Tournament("World Cup", "wc-2026", "2026", TournamentStatus.OPEN);
        setId(user, UUID.randomUUID());
        setId(otherUser, UUID.randomUUID());
        setId(tournament, UUID.randomUUID());
        PredictionPool pool = new PredictionPool("Pool", "desc", "ABC12345", user, tournament);
        UUID poolId = UUID.randomUUID();
        setId(pool, poolId);
        Team home = new Team(tournament, "Brazil", "BRA");
        Team away = new Team(tournament, "Spain", "ESP");
        Match futureMatch = new Match(
                tournament,
                home,
                away,
                Instant.parse("2027-06-01T13:00:00Z"),
                "GROUP_STAGE",
                "A",
                MatchStatus.SCHEDULED
        );
        Match closedMatch = new Match(
                tournament,
                away,
                home,
                Instant.parse("2027-06-01T11:00:00Z"),
                "GROUP_STAGE",
                "A",
                MatchStatus.SCHEDULED
        );
        setId(futureMatch, UUID.randomUUID());
        setId(closedMatch, UUID.randomUUID());
        Prediction ownFuturePrediction = new Prediction(pool, futureMatch, user, 2, 1, Instant.parse("2027-05-31T10:00:00Z"));
        Prediction otherFuturePrediction = new Prediction(pool, futureMatch, otherUser, 1, 0, Instant.parse("2027-05-31T10:01:00Z"));
        Prediction otherClosedPrediction = new Prediction(pool, closedMatch, otherUser, 0, 1, Instant.parse("2027-05-31T10:02:00Z"));
        setId(ownFuturePrediction, UUID.randomUUID());
        setId(otherFuturePrediction, UUID.randomUUID());
        setId(otherClosedPrediction, UUID.randomUUID());

        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(userAccountRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, user.getId()))
                .thenReturn(Optional.of(new PoolMembership(pool, user, PoolRole.OWNER)));
        when(predictionRepository.findAllForPoolOrderByKickoffAt(poolId))
                .thenReturn(List.of(otherClosedPrediction, ownFuturePrediction, otherFuturePrediction));
        when(matchResultRepository.findAllByMatchIdIn(List.of(closedMatch.getId(), futureMatch.getId())))
                .thenReturn(List.of());

        List<PoolPredictionResponse> predictions = predictionSubmissionService.listVisiblePoolPredictions(poolId, "owner@example.com");

        assertThat(predictions).hasSize(2);
        assertThat(predictions)
                .extracting(response -> response.user().username())
                .containsExactly("bob", "owner");
        assertThat(predictions)
                .extracting(PoolPredictionResponse::mine)
                .containsExactly(false, true);
    }

    @Test
    void shouldRejectPredictionListWhenUserIsNotPoolMember() {
        UserAccount user = new UserAccount("owner", "owner@example.com", "hash", UserRole.USER);
        Tournament tournament = new Tournament("World Cup", "wc-2026", "2026", TournamentStatus.OPEN);
        PredictionPool pool = new PredictionPool("Pool", "desc", "ABC12345", user, tournament);
        UUID poolId = UUID.randomUUID();
        setId(user, UUID.randomUUID());

        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(userAccountRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> predictionSubmissionService.listVisiblePoolPredictions(poolId, "owner@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    void shouldRejectPredictionAfterKickoff() {
        UserAccount owner = new UserAccount("owner", "owner@example.com", "hash", UserRole.USER);
        Tournament tournament = new Tournament("World Cup", "wc-2026", "2026", TournamentStatus.OPEN);
        setId(owner, UUID.randomUUID());
        setId(tournament, UUID.randomUUID());
        PredictionPool pool = new PredictionPool("Pool", "desc", "ABC12345", owner, tournament);
        Team home = new Team(tournament, "Brazil", "BRA");
        Team away = new Team(tournament, "Spain", "ESP");
        Match startedMatch = new Match(
                tournament,
                home,
                away,
                Instant.parse("2027-06-01T11:00:00Z"),
                "GROUP",
                MatchStatus.SCHEDULED
        );

        UUID poolId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(startedMatch));

        assertThatThrownBy(() -> predictionSubmissionService.submit(
                new SubmitPredictionCommand(poolId, matchId, "owner@example.com", 1, 0)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void shouldRejectPredictionWhenUserIsNotPoolMember() {
        UserAccount owner = new UserAccount("owner", "owner@example.com", "hash", UserRole.USER);
        Tournament tournament = new Tournament("World Cup", "wc-2026", "2026", TournamentStatus.OPEN);
        setId(owner, UUID.randomUUID());
        setId(tournament, UUID.randomUUID());
        PredictionPool pool = new PredictionPool("Pool", "desc", "ABC12345", owner, tournament);
        Team home = new Team(tournament, "Brazil", "BRA");
        Team away = new Team(tournament, "Spain", "ESP");
        Match futureMatch = new Match(
                tournament,
                home,
                away,
                Instant.parse("2027-06-01T13:00:00Z"),
                "GROUP",
                MatchStatus.SCHEDULED
        );

        UUID poolId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(futureMatch));
        when(userAccountRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, owner.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> predictionSubmissionService.submit(
                new SubmitPredictionCommand(poolId, matchId, "owner@example.com", 1, 0)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    void shouldRejectPredictionWhenMatchTournamentDiffersFromPoolTournament() {
        UserAccount owner = new UserAccount("owner", "owner@example.com", "hash", UserRole.USER);
        Tournament poolTournament = new Tournament("World Cup", "wc-2026", "2026", TournamentStatus.OPEN);
        Tournament otherTournament = new Tournament("Euro", "euro-2028", "2028", TournamentStatus.OPEN);
        setId(poolTournament, UUID.randomUUID());
        setId(otherTournament, UUID.randomUUID());
        PredictionPool pool = new PredictionPool("Pool", "desc", "ABC12345", owner, poolTournament);
        Team home = new Team(otherTournament, "Italy", "ITA");
        Team away = new Team(otherTournament, "France", "FRA");
        Match futureMatch = new Match(
                otherTournament,
                home,
                away,
                Instant.parse("2027-06-01T13:00:00Z"),
                "GROUP",
                MatchStatus.SCHEDULED
        );

        UUID poolId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(futureMatch));

        assertThatThrownBy(() -> predictionSubmissionService.submit(
                new SubmitPredictionCommand(poolId, matchId, "owner@example.com", 1, 0)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void shouldRejectPredictionForAnotherMatchInSingleMatchPool() {
        UserAccount owner = new UserAccount("owner", "owner@example.com", "hash", UserRole.USER);
        Tournament tournament = new Tournament("World Cup", "wc-2026", "2026", TournamentStatus.OPEN);
        setId(owner, UUID.randomUUID());
        setId(tournament, UUID.randomUUID());
        Team home = new Team(tournament, "Brazil", "BRA");
        Team away = new Team(tournament, "Egypt", "EGY");
        Match poolMatch = new Match(
                tournament,
                home,
                away,
                Instant.parse("2027-06-01T13:00:00Z"),
                "FRIENDLY",
                MatchStatus.SCHEDULED
        );
        Match otherMatch = new Match(
                tournament,
                away,
                home,
                Instant.parse("2027-06-02T13:00:00Z"),
                "FRIENDLY",
                MatchStatus.SCHEDULED
        );
        UUID poolMatchId = UUID.randomUUID();
        UUID otherMatchId = UUID.randomUUID();
        setId(poolMatch, poolMatchId);
        setId(otherMatch, otherMatchId);
        PredictionPool pool = new PredictionPool("Pool", "desc", "ABC12345", owner, poolMatch);

        UUID poolId = UUID.randomUUID();
        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(matchRepository.findById(otherMatchId)).thenReturn(Optional.of(otherMatch));

        assertThatThrownBy(() -> predictionSubmissionService.submit(
                new SubmitPredictionCommand(poolId, otherMatchId, "owner@example.com", 1, 0)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Match does not belong to single-match pool");
                });
    }

    private static void setId(BaseEntity entity, UUID id) {
        try {
            Field field = BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
