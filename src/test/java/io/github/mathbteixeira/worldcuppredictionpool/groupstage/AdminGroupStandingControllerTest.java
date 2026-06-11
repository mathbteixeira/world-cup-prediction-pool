package io.github.mathbteixeira.worldcuppredictionpool.groupstage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.api.AdminGroupStandingController;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.application.GroupStandingScoringService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.StandingsRecalculationResult;
import io.github.mathbteixeira.worldcuppredictionpool.security.AppUserDetailsService;
import io.github.mathbteixeira.worldcuppredictionpool.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminGroupStandingController.class)
@Import(AdminGroupStandingControllerTest.MethodSecurityTestConfiguration.class)
class AdminGroupStandingControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GroupStandingScoringService groupStandingScoringService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    private String endpoint(UUID tournamentId) {
        return "/api/v1/admin/tournaments/" + tournamentId + "/groups/A/standings";
    }

    @Test
    void adminCanConfirmStandingsAndReceivesSummary() throws Exception {
        UUID tournamentId = UUID.randomUUID();
        List<UUID> order = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(groupStandingScoringService.confirmAndRecalculate(eq(tournamentId), eq("A"), any()))
                .thenReturn(new StandingsRecalculationResult("checksum-a", 5, 2, false));

        mockMvc.perform(put(endpoint(tournamentId))
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("teamIdsByPosition", order))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tournamentId").value(tournamentId.toString()))
                .andExpect(jsonPath("$.groupName").value("A"))
                .andExpect(jsonPath("$.resultChecksum").value("checksum-a"))
                .andExpect(jsonPath("$.scoredPredictions").value(5))
                .andExpect(jsonPath("$.affectedPools").value(2))
                .andExpect(jsonPath("$.idempotentReplay").value(false));
    }

    @Test
    void nonAdminIsForbidden() throws Exception {
        UUID tournamentId = UUID.randomUUID();
        mockMvc.perform(put(endpoint(tournamentId))
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "teamIdsByPosition", List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedIsRejected() throws Exception {
        UUID tournamentId = UUID.randomUUID();
        int status = mockMvc.perform(put(endpoint(tournamentId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "teamIdsByPosition", List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())))))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(401, 403);
    }
}