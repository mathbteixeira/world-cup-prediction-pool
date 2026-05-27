package io.github.mathbteixeira.worldcuppredictionpool.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mathbteixeira.worldcuppredictionpool.security.AppUserDetailsService;
import io.github.mathbteixeira.worldcuppredictionpool.security.JwtTokenService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.api.AdminMatchResultController;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.MatchResultScoringService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.RecalculationResult;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.UpsertMatchResultCommand;
import org.springframework.boot.test.context.TestConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMatchResultController.class)
@Import(AdminMatchResultControllerTest.MethodSecurityTestConfiguration.class)
class AdminMatchResultControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MatchResultScoringService matchResultScoringService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    @Test
    void adminCanUpsertResultAndReceivesExpectedJson() throws Exception {
        UUID matchId = UUID.randomUUID();
        String checksum = "checksum-abc";
        when(matchResultScoringService.upsertResultAndRecalculate(any()))
                .thenReturn(new RecalculationResult(matchId, checksum, 12, 3, false));

        mockMvc.perform(put(endpoint(matchId))
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "homeScore", 2,
                                "awayScore", 1,
                                "homePenaltyScore", 4,
                                "awayPenaltyScore", 3,
                                "finalResult", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.homeScore").value(2))
                .andExpect(jsonPath("$.awayScore").value(1))
                .andExpect(jsonPath("$.homePenaltyScore").value(4))
                .andExpect(jsonPath("$.awayPenaltyScore").value(3))
                .andExpect(jsonPath("$.finalResult").value(true))
                .andExpect(jsonPath("$.resultChecksum").value(checksum))
                .andExpect(jsonPath("$.scoredPredictions").value(12))
                .andExpect(jsonPath("$.affectedPools").value(3))
                .andExpect(jsonPath("$.idempotentReplay").value(false));
    }

    @Test
    void nonAdminAuthenticatedUserIsForbidden() throws Exception {
        UUID matchId = UUID.randomUUID();
        mockMvc.perform(put(endpoint(matchId))
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "homeScore", 1,
                                "awayScore", 0,
                                "finalResult", true
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        UUID matchId = UUID.randomUUID();
        int status = mockMvc.perform(put(endpoint(matchId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "homeScore", 1,
                                "awayScore", 0,
                                "finalResult", true
                        ))))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(401, 403);
    }

    @Test
    void missingRequiredScoresReturn400() throws Exception {
        UUID matchId = UUID.randomUUID();
        mockMvc.perform(put(endpoint(matchId))
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "awayScore", 1,
                                "finalResult", true
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(endpoint(matchId))
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "homeScore", 1,
                                "finalResult", true
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativeScoresReturn400() throws Exception {
        UUID matchId = UUID.randomUUID();
        mockMvc.perform(put(endpoint(matchId))
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "homeScore", -1,
                                "awayScore", 1,
                                "finalResult", true
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(endpoint(matchId))
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "homeScore", 1,
                                "awayScore", -1,
                                "finalResult", true
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pathMatchIdIsUsedNotBodyMatchId() throws Exception {
        UUID pathMatchId = UUID.randomUUID();
        UUID bodyMatchId = UUID.randomUUID();
        when(matchResultScoringService.upsertResultAndRecalculate(any()))
                .thenReturn(new RecalculationResult(pathMatchId, "checksum", 1, 1, false));

        mockMvc.perform(put(endpoint(pathMatchId))
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "matchId", bodyMatchId.toString(),
                                "homeScore", 1,
                                "awayScore", 0,
                                "finalResult", true
                        ))))
                .andExpect(status().isOk());

        ArgumentCaptor<UpsertMatchResultCommand> commandCaptor = ArgumentCaptor.forClass(UpsertMatchResultCommand.class);
        verify(matchResultScoringService).upsertResultAndRecalculate(commandCaptor.capture());
        assertThat(commandCaptor.getValue().matchId()).isEqualTo(pathMatchId);
    }

    private static String endpoint(UUID matchId) {
        return "/api/v1/admin/matches/" + matchId + "/result";
    }
}
