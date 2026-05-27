package io.github.mathbteixeira.worldcuppredictionpool.prediction;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.application.PredictionSubmissionService;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.application.SubmitPredictionCommand;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence.PredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
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
    private UserAccountRepository userAccountRepository;

    private PredictionSubmissionService predictionSubmissionService;

    @BeforeEach
    void setUp() {
        predictionSubmissionService = new PredictionSubmissionService(
                predictionRepository,
                predictionPoolRepository,
                poolMembershipRepository,
                matchRepository,
                userAccountRepository,
                Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC));
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
                Instant.parse("2026-06-01T11:00:00Z"),
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
                Instant.parse("2026-06-01T13:00:00Z"),
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
                Instant.parse("2026-06-01T13:00:00Z"),
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
