package io.github.mathbteixeira.worldcuppredictionpool.pool;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.api.CreatePoolRequest;
import io.github.mathbteixeira.worldcuppredictionpool.pool.api.PoolSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.pool.application.PoolService;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolRole;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PoolServiceTest {

    @Mock
    private PredictionPoolRepository predictionPoolRepository;

    @Mock
    private PoolMembershipRepository poolMembershipRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private PoolService poolService;

    @Test
    void shouldCreatePoolAndRegisterOwnerMembership() {
        Tournament tournament = new Tournament("World Cup", "world-cup-2026", "2026", TournamentStatus.OPEN);
        UUID tournamentId = UUID.randomUUID();
        UserAccount owner = new UserAccount("ana", "ana@example.com", "encoded", UserRole.USER);
        when(userAccountRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(owner));
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(predictionPoolRepository.save(any(PredictionPool.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(poolMembershipRepository.save(any(PoolMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PoolSummaryResponse response = poolService.createPool(
                new CreatePoolRequest("Office Pool", "Qatar 2026", CreatePoolRequest.PoolMode.TOURNAMENT, tournamentId, null, null),
                "ana@example.com"
        );

        assertThat(response.name()).isEqualTo("Office Pool");
        assertThat(response.poolScope()).isEqualTo("TOURNAMENT");
        assertThat(response.singleMatchId()).isNull();
        assertThat(response.membershipRole()).isEqualTo("OWNER");
        assertThat(response.inviteCode()).hasSize(8);

        ArgumentCaptor<PoolMembership> membershipCaptor = ArgumentCaptor.forClass(PoolMembership.class);
        verify(poolMembershipRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getRole().name()).isEqualTo("OWNER");
    }

    @Test
    void shouldCreateSingleMatchPoolFromExistingMatch() {
        Tournament tournament = new Tournament("World Cup", "world-cup-2026", "2026", TournamentStatus.OPEN);
        UUID tournamentId = UUID.randomUUID();
        setId(tournament, tournamentId);
        UserAccount owner = new UserAccount("ana", "ana@example.com", "encoded", UserRole.USER);
        Team home = new Team(tournament, "Brazil", "BRA");
        Team away = new Team(tournament, "Egypt", "EGY");
        Match match = new Match(
                tournament,
                home,
                away,
                Instant.parse("2027-06-01T13:00:00Z"),
                "FRIENDLY",
                MatchStatus.SCHEDULED
        );
        UUID matchId = UUID.randomUUID();
        setId(match, matchId);
        when(userAccountRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(owner));
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(predictionPoolRepository.save(any(PredictionPool.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(poolMembershipRepository.save(any(PoolMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PoolSummaryResponse response = poolService.createPool(
                new CreatePoolRequest("Family Match", null, CreatePoolRequest.PoolMode.SINGLE_MATCH, null, matchId, null),
                "ana@example.com"
        );

        assertThat(response.poolScope()).isEqualTo("SINGLE_MATCH");
        assertThat(response.tournamentId()).isEqualTo(tournamentId);
        assertThat(response.singleMatchId()).isEqualTo(matchId);
        assertThat(response.membershipRole()).isEqualTo("OWNER");
    }

    @Test
    void shouldCreateSingleMatchPoolWithCustomMatch() {
        UserAccount owner = new UserAccount("ana", "ana@example.com", "encoded", UserRole.USER);
        when(userAccountRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(owner));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match match = invocation.getArgument(0);
            setId(match, UUID.randomUUID());
            setId(match.getTournament(), UUID.randomUUID());
            return match;
        });
        when(predictionPoolRepository.save(any(PredictionPool.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(poolMembershipRepository.save(any(PoolMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PoolSummaryResponse response = poolService.createPool(
                new CreatePoolRequest(
                        "Family Match",
                        null,
                        CreatePoolRequest.PoolMode.SINGLE_MATCH,
                        null,
                        null,
                        new CreatePoolRequest.CustomMatchRequest(
                                "Brazil",
                                "Egypt",
                                Instant.parse("2027-06-01T13:00:00Z"),
                                "Friendly"
                        )
                ),
                "ana@example.com"
        );

        assertThat(response.poolScope()).isEqualTo("SINGLE_MATCH");
        assertThat(response.singleMatchId()).isNotNull();
        assertThat(response.tournamentId()).isNotNull();
    }

    @Test
    void shouldRejectSingleMatchPoolWithoutExactlyOneMatchSource() {
        UserAccount owner = new UserAccount("ana", "ana@example.com", "encoded", UserRole.USER);
        when(userAccountRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> poolService.createPool(
                new CreatePoolRequest("Family Match", null, CreatePoolRequest.PoolMode.SINGLE_MATCH, null, null, null),
                "ana@example.com"
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo("Single-match pool must reference exactly one existing or custom match");
                });

        verify(predictionPoolRepository, never()).save(any(PredictionPool.class));
    }

    @Test
    void shouldRejectInvalidInviteCode() {
        Tournament tournament = new Tournament("World Cup", "world-cup-2026", "2026", TournamentStatus.OPEN);
        UserAccount owner = new UserAccount("owner", "owner@example.com", "encoded", UserRole.USER);
        PredictionPool pool = new PredictionPool("Office Pool", "Qatar 2026", "ABCDEFGH", owner, tournament);
        UserAccount user = new UserAccount("ana", "ana@example.com", "encoded", UserRole.USER);
        UUID userId = UUID.randomUUID();
        setId(user, userId);
        UUID poolId = UUID.randomUUID();
        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(userAccountRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> poolService.joinPool(poolId, "WRONG123", "ana@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getReason()).isEqualTo("Invalid invite code");
                });

        verify(poolMembershipRepository, never()).save(any(PoolMembership.class));
    }

    @Test
    void shouldReturnConflictWhenUserIsAlreadyMemberBeforeInviteCodeValidation() {
        Tournament tournament = new Tournament("World Cup", "world-cup-2026", "2026", TournamentStatus.OPEN);
        UserAccount owner = new UserAccount("owner", "owner@example.com", "encoded", UserRole.USER);
        PredictionPool pool = new PredictionPool("Office Pool", "Qatar 2026", "ABCDEFGH", owner, tournament);
        UserAccount user = new UserAccount("ana", "ana@example.com", "encoded", UserRole.USER);
        UUID userId = UUID.randomUUID();
        setId(user, userId);
        UUID poolId = UUID.randomUUID();
        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(userAccountRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, userId))
                .thenReturn(Optional.of(new PoolMembership(pool, user, PoolRole.MEMBER)));

        assertThatThrownBy(() -> poolService.joinPool(poolId, "WRONG123", "ana@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("User is already a member of this pool");
                });

        verify(poolMembershipRepository, never()).save(any(PoolMembership.class));
    }

    @Test
    void shouldReturnPoolForMember() {
        Tournament tournament = new Tournament("World Cup", "world-cup-2026", "2026", TournamentStatus.OPEN);
        UUID tournamentId = UUID.randomUUID();
        setId(tournament, tournamentId);
        UserAccount user = new UserAccount("ana", "ana@example.com", "encoded", UserRole.USER);
        UUID userId = UUID.randomUUID();
        setId(user, userId);
        PredictionPool pool = new PredictionPool("Office Pool", "Qatar 2026", "ABCDEFGH", user, tournament);
        UUID poolId = UUID.randomUUID();
        setId(pool, poolId);

        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(userAccountRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, userId))
                .thenReturn(Optional.of(new PoolMembership(pool, user, PoolRole.OWNER)));

        PoolSummaryResponse response = poolService.getPool(poolId, "ana@example.com");

        assertThat(response.id()).isEqualTo(poolId);
        assertThat(response.tournamentId()).isEqualTo(tournamentId);
        assertThat(response.name()).isEqualTo("Office Pool");
        assertThat(response.membershipRole()).isEqualTo("OWNER");
    }

    @Test
    void shouldJoinPoolByInviteCode() {
        Tournament tournament = new Tournament("World Cup", "world-cup-2026", "2026", TournamentStatus.OPEN);
        UserAccount owner = new UserAccount("owner", "owner@example.com", "encoded", UserRole.USER);
        PredictionPool pool = new PredictionPool("Office Pool", "Qatar 2026", "ABCDEFGH", owner, tournament);
        UUID poolId = UUID.randomUUID();
        setId(pool, poolId);
        UserAccount user = new UserAccount("ana", "ana@example.com", "encoded", UserRole.USER);
        UUID userId = UUID.randomUUID();
        setId(user, userId);

        when(predictionPoolRepository.findByInviteCodeIgnoreCase("ABCDEFGH")).thenReturn(Optional.of(pool));
        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(userAccountRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, userId)).thenReturn(Optional.empty());
        when(poolMembershipRepository.save(any(PoolMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PoolSummaryResponse response = poolService.joinPoolByInviteCode("ABCDEFGH", "ana@example.com");

        assertThat(response.id()).isEqualTo(poolId);
        assertThat(response.membershipRole()).isEqualTo("MEMBER");
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
