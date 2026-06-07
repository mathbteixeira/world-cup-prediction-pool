package io.github.mathbteixeira.worldcuppredictionpool.pool;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.api.ManagedParticipantResponse;
import io.github.mathbteixeira.worldcuppredictionpool.pool.application.ManagedParticipantService;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.ManagedParticipant;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolRole;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.ManagedParticipantRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence.PredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.LeaderboardEntryRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.PredictionCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.ScoreEventRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
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

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagedParticipantServiceTest {

    @Mock
    private ManagedParticipantRepository managedParticipantRepository;

    @Mock
    private PredictionPoolRepository predictionPoolRepository;

    @Mock
    private PoolMembershipRepository poolMembershipRepository;

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private ScoreEventRepository scoreEventRepository;

    @Mock
    private PredictionCurrentScoreRepository predictionCurrentScoreRepository;

    @Mock
    private LeaderboardEntryRepository leaderboardEntryRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    private ManagedParticipantService service;

    @BeforeEach
    void setUp() {
        service = new ManagedParticipantService(
                managedParticipantRepository,
                predictionPoolRepository,
                poolMembershipRepository,
                predictionRepository,
                scoreEventRepository,
                predictionCurrentScoreRepository,
                leaderboardEntryRepository,
                userAccountRepository
        );
    }

    @Test
    void ownerCanCreateManagedParticipantInSingleMatchPool() {
        Fixture fixture = singleMatchFixture();
        when(predictionPoolRepository.findById(fixture.poolId)).thenReturn(Optional.of(fixture.pool));
        when(userAccountRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(fixture.owner));
        when(poolMembershipRepository.findByPoolIdAndUserId(fixture.poolId, fixture.owner.getId()))
                .thenReturn(Optional.of(new PoolMembership(fixture.pool, fixture.owner, PoolRole.OWNER)));
        when(managedParticipantRepository.existsByPoolIdAndDisplayNameIgnoreCase(fixture.poolId, "Grandma")).thenReturn(false);
        when(managedParticipantRepository.save(any(ManagedParticipant.class))).thenAnswer(invocation -> {
            ManagedParticipant participant = invocation.getArgument(0);
            setId(participant, UUID.randomUUID());
            return participant;
        });

        ManagedParticipantResponse response = service.create(fixture.poolId, " Grandma ", "owner@example.com");

        assertThat(response.poolId()).isEqualTo(fixture.poolId);
        assertThat(response.name()).isEqualTo("Grandma");
    }

    @Test
    void nonOwnerCannotCreateManagedParticipant() {
        Fixture fixture = singleMatchFixture();
        UserAccount member = new UserAccount("member", "member@example.com", "hash", UserRole.USER);
        setId(member, UUID.randomUUID());
        when(predictionPoolRepository.findById(fixture.poolId)).thenReturn(Optional.of(fixture.pool));
        when(userAccountRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(member));
        when(poolMembershipRepository.findByPoolIdAndUserId(fixture.poolId, member.getId()))
                .thenReturn(Optional.of(new PoolMembership(fixture.pool, member, PoolRole.MEMBER)));

        assertThatThrownBy(() -> service.create(fixture.poolId, "Grandma", "member@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void managedParticipantsAreNotAllowedInTournamentPools() {
        Tournament tournament = new Tournament("World Cup", "wc-2026", "2026", TournamentStatus.OPEN);
        UserAccount owner = new UserAccount("owner", "owner@example.com", "hash", UserRole.USER);
        UUID poolId = UUID.randomUUID();
        setId(owner, UUID.randomUUID());
        PredictionPool pool = new PredictionPool("Pool", "desc", "ABC12345", owner, tournament);
        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));

        assertThatThrownBy(() -> service.create(poolId, "Grandma", "owner@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo("Managed participants are supported only for single-match pools");
                });
    }

    private static Fixture singleMatchFixture() {
        Tournament tournament = new Tournament("World Cup", "wc-2026", "2026", TournamentStatus.OPEN);
        setId(tournament, UUID.randomUUID());
        UserAccount owner = new UserAccount("owner", "owner@example.com", "hash", UserRole.USER);
        setId(owner, UUID.randomUUID());
        Team home = new Team(tournament, "Brazil", "BRA");
        Team away = new Team(tournament, "Spain", "ESP");
        Match match = new Match(tournament, home, away, Instant.parse("2027-06-01T13:00:00Z"), "FRIENDLY", MatchStatus.SCHEDULED);
        setId(match, UUID.randomUUID());
        PredictionPool pool = new PredictionPool("Pool", "desc", "ABC12345", owner, match);
        UUID poolId = UUID.randomUUID();
        setId(pool, poolId);
        return new Fixture(owner, pool, poolId);
    }

    private record Fixture(UserAccount owner, PredictionPool pool, UUID poolId) {
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
