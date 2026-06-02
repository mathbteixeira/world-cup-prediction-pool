package io.github.mathbteixeira.worldcuppredictionpool.tournament;

import io.github.mathbteixeira.worldcuppredictionpool.security.AppUserDetailsService;
import io.github.mathbteixeira.worldcuppredictionpool.security.JwtTokenService;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.MatchSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TeamSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TournamentMatchController;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.application.TournamentMatchService;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TournamentMatchController.class)
class TournamentMatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TournamentMatchService tournamentMatchService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    @Test
    void authenticatedUserCanListMatches() throws Exception {
        UUID tournamentId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        TeamSummaryResponse home = new TeamSummaryResponse(UUID.randomUUID(), "Mexico", "MEX");
        TeamSummaryResponse away = new TeamSummaryResponse(UUID.randomUUID(), "South Africa", "RSA");

        when(tournamentMatchService.listMatches(eq(tournamentId), any(), any(), any(), any(), any(), any(), eq(false)))
                .thenReturn(List.of(new MatchSummaryResponse(
                        matchId,
                        tournamentId,
                        home,
                        away,
                        null,
                        null,
                        Instant.parse("2026-06-11T16:00:00Z"),
                        "GROUP_STAGE",
                        "A",
                        MatchStatus.SCHEDULED,
                        null,
                        true
                )));

        mockMvc.perform(get("/api/v1/tournaments/" + tournamentId + "/matches")
                        .with(user("user@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matchId").value(matchId.toString()))
                .andExpect(jsonPath("$[0].tournamentId").value(tournamentId.toString()))
                .andExpect(jsonPath("$[0].homeTeam.fifaCode").value("MEX"))
                .andExpect(jsonPath("$[0].awayTeam.fifaCode").value("RSA"))
                .andExpect(jsonPath("$[0].homePlaceholder").doesNotExist())
                .andExpect(jsonPath("$[0].awayPlaceholder").doesNotExist())
                .andExpect(jsonPath("$[0].stage").value("GROUP_STAGE"))
                .andExpect(jsonPath("$[0].groupName").value("A"))
                .andExpect(jsonPath("$[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$[0].predictionOpen").value(true));
    }

    @Test
    void queryParametersAreForwardedToService() throws Exception {
        UUID tournamentId = UUID.randomUUID();
        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);

        when(tournamentMatchService.listMatches(
                eq(tournamentId),
                eq(MatchStatus.SCHEDULED),
                eq("GROUP_STAGE"),
                eq("A"),
                eq("MEX"),
                fromCaptor.capture(),
                toCaptor.capture(),
                eq(true)
        )).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tournaments/" + tournamentId + "/matches")
                        .param("status", "SCHEDULED")
                        .param("stage", "GROUP_STAGE")
                        .param("group", "A")
                        .param("team", "MEX")
                        .param("from", "2026-06-11T00:00:00Z")
                        .param("to", "2026-06-12T00:00:00Z")
                        .param("predictableOnly", "true")
                        .with(user("user@example.com")))
                .andExpect(status().isOk());

        assertThat(fromCaptor.getValue()).isEqualTo(Instant.parse("2026-06-11T00:00:00Z"));
        assertThat(toCaptor.getValue()).isEqualTo(Instant.parse("2026-06-12T00:00:00Z"));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        UUID tournamentId = UUID.randomUUID();

        int status = mockMvc.perform(get("/api/v1/tournaments/" + tournamentId + "/matches"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(401, 403);
    }
}
