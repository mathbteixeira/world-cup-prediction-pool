package io.github.mathbteixeira.worldcuppredictionpool.pool;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.pool.api.PoolController;
import io.github.mathbteixeira.worldcuppredictionpool.pool.application.PoolLeaderboardService;
import io.github.mathbteixeira.worldcuppredictionpool.pool.application.PoolService;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolRole;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.domain.LeaderboardEntry;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.LeaderboardEntryRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserRole;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PoolController.class)
@Import(PoolLeaderboardService.class)
class PoolLeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PoolService poolService;

    @MockBean
    private LeaderboardEntryRepository leaderboardEntryRepository;

    @MockBean
    private PoolMembershipRepository poolMembershipRepository;

    @MockBean
    private UserAccountRepository userAccountRepository;

    @Test
    void poolMemberCanViewLeaderboard() throws Exception {
        UUID poolId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UserAccount viewer = user("viewer", "viewer@example.com", viewerId);
        UserAccount alice = user("alice", "alice@example.com", UUID.randomUUID());
        UserAccount bob = user("bob", "bob@example.com", UUID.randomUUID());
        PredictionPool pool = pool(poolId, viewer);

        when(userAccountRepository.findByEmailIgnoreCase("viewer@example.com")).thenReturn(Optional.of(viewer));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, viewerId))
                .thenReturn(Optional.of(new PoolMembership(pool, viewer, PoolRole.MEMBER)));
        when(leaderboardEntryRepository.findAllByPoolIdOrderByRankPositionAsc(poolId))
                .thenReturn(List.of(
                        new LeaderboardEntry(pool, alice, 9, 1, Instant.parse("2026-06-10T21:00:00Z")),
                        new LeaderboardEntry(pool, bob, 4, 2, Instant.parse("2026-06-10T21:00:00Z"))
                ));

        mockMvc.perform(get(endpoint(poolId)).with(user("viewer@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].poolId").value(poolId.toString()))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].rankPosition").value(1))
                .andExpect(jsonPath("$[1].username").value("bob"))
                .andExpect(jsonPath("$[1].rankPosition").value(2));
    }

    @Test
    void nonMemberGets403() throws Exception {
        UUID poolId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UserAccount viewer = user("viewer", "viewer@example.com", viewerId);

        when(userAccountRepository.findByEmailIgnoreCase("viewer@example.com")).thenReturn(Optional.of(viewer));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, viewerId)).thenReturn(Optional.empty());

        mockMvc.perform(get(endpoint(poolId)).with(user("viewer@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        UUID poolId = UUID.randomUUID();
        int status = mockMvc.perform(get(endpoint(poolId)))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(401, 403);
    }

    @Test
    void entriesAreReturnedInRankOrder() throws Exception {
        UUID poolId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UserAccount viewer = user("viewer", "viewer@example.com", viewerId);
        PredictionPool pool = pool(poolId, viewer);
        UserAccount alice = user("alice", "alice@example.com", UUID.randomUUID());
        UserAccount bob = user("bob", "bob@example.com", UUID.randomUUID());
        UserAccount carol = user("carol", "carol@example.com", UUID.randomUUID());

        when(userAccountRepository.findByEmailIgnoreCase("viewer@example.com")).thenReturn(Optional.of(viewer));
        when(poolMembershipRepository.findByPoolIdAndUserId(poolId, viewerId))
                .thenReturn(Optional.of(new PoolMembership(pool, viewer, PoolRole.MEMBER)));
        when(leaderboardEntryRepository.findAllByPoolIdOrderByRankPositionAsc(poolId))
                .thenReturn(List.of(
                        new LeaderboardEntry(pool, alice, 10, 1, Instant.parse("2026-06-10T21:00:00Z")),
                        new LeaderboardEntry(pool, bob, 8, 2, Instant.parse("2026-06-10T21:00:00Z")),
                        new LeaderboardEntry(pool, carol, 8, 2, Instant.parse("2026-06-10T21:00:00Z"))
                ));

        mockMvc.perform(get(endpoint(poolId)).with(user("viewer@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rankPosition").value(1))
                .andExpect(jsonPath("$[1].rankPosition").value(2))
                .andExpect(jsonPath("$[2].rankPosition").value(2));
    }

    private static String endpoint(UUID poolId) {
        return "/api/v1/pools/" + poolId + "/leaderboard";
    }

    private static UserAccount user(String username, String email, UUID id) {
        UserAccount user = new UserAccount(username, email, "hash", UserRole.USER);
        setId(user, id);
        return user;
    }

    private static PredictionPool pool(UUID poolId, UserAccount owner) {
        Tournament tournament = new Tournament("World Cup", "wc-2026", 2026, TournamentStatus.OPEN);
        setId(tournament, UUID.randomUUID());
        PredictionPool pool = new PredictionPool("Office", "desc", "INVITE123", owner, tournament);
        setId(pool, poolId);
        return pool;
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
