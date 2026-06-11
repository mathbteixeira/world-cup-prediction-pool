package io.github.mathbteixeira.worldcuppredictionpool.finalranking;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.api.AdminTournamentRankingController;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.application.TournamentRankingScoringService;
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

import java.util.HashMap;
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

@WebMvcTest(AdminTournamentRankingController.class)
@Import(AdminTournamentRankingControllerTest.MethodSecurityTestConfiguration.class)
class AdminTournamentRankingControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TournamentRankingScoringService tournamentRankingScoringService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    private String endpoint(UUID tournamentId) {
        return "/api/v1/admin/tournaments/" + tournamentId + "/final-ranking";
    }

    private Map<String, Object> podiumBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("championTeamId", UUID.randomUUID().toString());
        body.put("runnerUpTeamId", UUID.randomUUID().toString());
        body.put("thirdPlaceTeamId", UUID.randomUUID().toString());
        body.put("fourthPlaceTeamId", UUID.randomUUID().toString());
        return body;
    }

    @Test
    void adminCanConfirmRankingAndReceivesSummary() throws Exception {
        UUID tournamentId = UUID.randomUUID();
        when(tournamentRankingScoringService.confirmAndRecalculate(eq(tournamentId), any(), any(), any(), any()))
                .thenReturn(new StandingsRecalculationResult("checksum-r", 7, 3, false));

        mockMvc.perform(put(endpoint(tournamentId))
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(podiumBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tournamentId").value(tournamentId.toString()))
                .andExpect(jsonPath("$.resultChecksum").value("checksum-r"))
                .andExpect(jsonPath("$.scoredPredictions").value(7))
                .andExpect(jsonPath("$.affectedPools").value(3));
    }

    @Test
    void nonAdminIsForbidden() throws Exception {
        UUID tournamentId = UUID.randomUUID();
        mockMvc.perform(put(endpoint(tournamentId))
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(podiumBody())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedIsRejected() throws Exception {
        UUID tournamentId = UUID.randomUUID();
        int status = mockMvc.perform(put(endpoint(tournamentId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(podiumBody())))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(401, 403);
    }
}