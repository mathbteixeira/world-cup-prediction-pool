package io.github.mathbteixeira.worldcuppredictionpool.pool;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.api.AdminPoolSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.pool.api.PoolMemberResponse;
import io.github.mathbteixeira.worldcuppredictionpool.pool.application.AdminPoolModerationService;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolRole;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence.PredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.LeaderboardEntryRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.PredictionCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.ScoreEventRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPoolModerationServiceTest {

    @Mock
    private PredictionPoolRepository predictionPoolRepository;

    @Mock
    private PoolMembershipRepository poolMembershipRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private ScoreEventRepository scoreEventRepository;

    @Mock
    private PredictionCurrentScoreRepository predictionCurrentScoreRepository;

    @Mock
    private LeaderboardEntryRepository leaderboardEntryRepository;

    private AdminPoolModerationService service;
    private UUID poolId;
    private PredictionPool pool;
    private UserAccount owner;
    private UserAccount member;

    @BeforeEach
    void setUp() {
        service = new AdminPoolModerationService(
                predictionPoolRepository,
                poolMembershipRepository,
                userAccountRepository,
                predictionRepository,
                scoreEventRepository,
                predictionCurrentScoreRepository,
                leaderboardEntryRepository
        );

        Tournament tournament = new Tournament("World Cup", "world-cup-2026", "2026", TournamentStatus.OPEN);
        setId(tournament, UUID.randomUUID());
        owner = user("owner", "owner@example.com");
        member = user("member", "member@example.com");
        pool = new PredictionPool("Family", "desc", "INVITE01", owner, tournament);
        poolId = UUID.randomUUID();
        setId(pool, poolId);
    }

    @Test
    void shouldAddRegisteredUserAsMember() {
        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(userAccountRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(member));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, member.getId())).thenReturn(Optional.empty());
        when(poolMembershipRepository.save(org.mockito.ArgumentMatchers.any(PoolMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PoolMemberResponse response = service.addMember(poolId, "member@example.com");

        assertThat(response.userId()).isEqualTo(member.getId());
        assertThat(response.role()).isEqualTo("MEMBER");
    }

    @Test
    void shouldTransferOwnershipAndKeepPreviousOwnerAsMember() {
        PoolMembership oldOwnerMembership = new PoolMembership(pool, owner, PoolRole.OWNER);
        PoolMembership newOwnerMembership = new PoolMembership(pool, member, PoolRole.MEMBER);
        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(userAccountRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(member));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, member.getId())).thenReturn(Optional.of(newOwnerMembership));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, owner.getId())).thenReturn(Optional.of(oldOwnerMembership));
        when(predictionPoolRepository.save(pool)).thenReturn(pool);
        when(poolMembershipRepository.countByPoolId(poolId)).thenReturn(2L);

        AdminPoolSummaryResponse response = service.transferOwnership(poolId, "member@example.com");

        assertThat(response.owner().email()).isEqualTo("member@example.com");
        assertThat(pool.getOwner()).isEqualTo(member);
        assertThat(newOwnerMembership.getRole()).isEqualTo(PoolRole.OWNER);
        assertThat(oldOwnerMembership.getRole()).isEqualTo(PoolRole.MEMBER);
    }

    @Test
    void shouldRejectRemovingPoolOwner() {
        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(userAccountRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, owner.getId()))
                .thenReturn(Optional.of(new PoolMembership(pool, owner, PoolRole.OWNER)));

        assertThatThrownBy(() -> service.removeMember(poolId, owner.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void shouldRemoveMemberAndMemberScoringData() {
        UUID predictionId = UUID.randomUUID();
        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));
        when(userAccountRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, member.getId()))
                .thenReturn(Optional.of(new PoolMembership(pool, member, PoolRole.MEMBER)));
        when(predictionRepository.findIdsByPoolIdAndUserId(poolId, member.getId())).thenReturn(List.of(predictionId));

        service.removeMember(poolId, member.getId());

        verify(scoreEventRepository).deleteByPredictionIdIn(List.of(predictionId));
        verify(predictionCurrentScoreRepository).deleteByPredictionIdIn(List.of(predictionId));
        verify(leaderboardEntryRepository).deleteByPoolIdAndUserId(poolId, member.getId());
        verify(predictionRepository).deleteByPoolIdAndUserId(poolId, member.getId());
        verify(poolMembershipRepository).deleteByPoolIdAndUserId(poolId, member.getId());
    }

    private UserAccount user(String username, String email) {
        UserAccount user = new UserAccount(username, email, "hash", UserRole.USER);
        setId(user, UUID.randomUUID());
        return user;
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
