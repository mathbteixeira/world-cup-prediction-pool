package io.github.mathbteixeira.worldcuppredictionpool.pool;

import io.github.mathbteixeira.worldcuppredictionpool.pool.api.CreatePoolRequest;
import io.github.mathbteixeira.worldcuppredictionpool.pool.api.PoolSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.pool.application.PoolService;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserRole;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

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

    @InjectMocks
    private PoolService poolService;

    @Test
    void shouldCreatePoolAndRegisterOwnerMembership() {
        UserAccount owner = new UserAccount("ana", "ana@example.com", "encoded", UserRole.USER);
        when(userAccountRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(owner));
        when(predictionPoolRepository.save(any(PredictionPool.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(poolMembershipRepository.save(any(PoolMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PoolSummaryResponse response = poolService.createPool(new CreatePoolRequest("Office Pool", "Qatar 2026"), "ana@example.com");

        assertThat(response.name()).isEqualTo("Office Pool");
        assertThat(response.membershipRole()).isEqualTo("OWNER");
        assertThat(response.inviteCode()).hasSize(8);

        ArgumentCaptor<PoolMembership> membershipCaptor = ArgumentCaptor.forClass(PoolMembership.class);
        verify(poolMembershipRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getRole().name()).isEqualTo("OWNER");
    }

    @Test
    void shouldRejectInvalidInviteCodeBeforeLoadingUser() {
        UserAccount owner = new UserAccount("owner", "owner@example.com", "encoded", UserRole.USER);
        PredictionPool pool = new PredictionPool("Office Pool", "Qatar 2026", "ABCDEFGH", owner);
        UUID poolId = UUID.randomUUID();
        when(predictionPoolRepository.findById(poolId)).thenReturn(Optional.of(pool));

        assertThatThrownBy(() -> poolService.joinPool(poolId, "WRONG123", "ana@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");

        verify(userAccountRepository, never()).findByEmailIgnoreCase("ana@example.com");
    }
}
