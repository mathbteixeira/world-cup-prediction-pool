package io.github.mathbteixeira.worldcuppredictionpool.tournament;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mathbteixeira.worldcuppredictionpool.security.AppUserDetailsService;
import io.github.mathbteixeira.worldcuppredictionpool.security.JwtTokenService;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.AdminMatchParticipantController;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.MatchSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TeamSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.application.MatchParticipantResolutionService;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.application.ResolveMatchParticipantsCommand;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
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

@WebMvcTest(AdminMatchParticipantController.class)
@Import(AdminMatchParticipantControllerTest.MethodSecurityTestConfiguration.class)
class AdminMatchParticipantControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfiguration {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MatchParticipantResolutionService matchParticipantResolutionService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    @Test
    void adminCanResolveParticipants() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        UUID homeTeamId = UUID.randomUUID();
        UUID awayTeamId = UUID.randomUUID();
        when(matchParticipantResolutionService.resolve(any()))
                .thenReturn(new MatchSummaryResponse(
                        matchId,
                        tournamentId,
                        new TeamSummaryResponse(homeTeamId, "Brazil", "BRA"),
                        new TeamSummaryResponse(awayTeamId, "Spain", "ESP"),
                        null,
                        null,
                        Instant.parse("2026-06-29T16:00:00Z"),
                        "ROUND_OF_32",
                        null,
                        MatchStatus.SCHEDULED,
                        null,
                        true
                ));

        mockMvc.perform(put(endpoint(matchId))
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "homeTeamId", homeTeamId,
                                "awayTeamId", awayTeamId
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.homeTeam.fifaCode").value("BRA"))
                .andExpect(jsonPath("$.awayTeam.fifaCode").value("ESP"))
                .andExpect(jsonPath("$.homePlaceholder").doesNotExist())
                .andExpect(jsonPath("$.awayPlaceholder").doesNotExist())
                .andExpect(jsonPath("$.predictionOpen").value(true));

        ArgumentCaptor<ResolveMatchParticipantsCommand> commandCaptor = ArgumentCaptor.forClass(ResolveMatchParticipantsCommand.class);
        verify(matchParticipantResolutionService).resolve(commandCaptor.capture());
        assertThat(commandCaptor.getValue().matchId()).isEqualTo(matchId);
        assertThat(commandCaptor.getValue().homeTeamId()).isEqualTo(homeTeamId);
        assertThat(commandCaptor.getValue().awayTeamId()).isEqualTo(awayTeamId);
    }

    @Test
    void nonAdminAuthenticatedUserIsForbidden() throws Exception {
        UUID matchId = UUID.randomUUID();
        mockMvc.perform(put(endpoint(matchId))
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "homeTeamId", UUID.randomUUID(),
                                "awayTeamId", UUID.randomUUID()
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingRequiredTeamIdsReturn400() throws Exception {
        UUID matchId = UUID.randomUUID();
        mockMvc.perform(put(endpoint(matchId))
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("homeTeamId", UUID.randomUUID()))))
                .andExpect(status().isBadRequest());
    }

    private static String endpoint(UUID matchId) {
        return "/api/v1/admin/matches/" + matchId + "/participants";
    }
}
