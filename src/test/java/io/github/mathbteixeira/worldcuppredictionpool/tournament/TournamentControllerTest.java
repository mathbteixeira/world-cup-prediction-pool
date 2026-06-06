package io.github.mathbteixeira.worldcuppredictionpool.tournament;

import io.github.mathbteixeira.worldcuppredictionpool.security.AppUserDetailsService;
import io.github.mathbteixeira.worldcuppredictionpool.security.JwtTokenService;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TournamentController;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TournamentSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.application.TournamentService;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TournamentController.class)
class TournamentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TournamentService tournamentService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    @Test
    void authenticatedUserCanListTournaments() throws Exception {
        UUID tournamentId = UUID.randomUUID();
        when(tournamentService.listTournaments())
                .thenReturn(List.of(new TournamentSummaryResponse(
                        tournamentId,
                        "National Team Friendlies",
                        "national-team-friendlies",
                        "2026",
                        TournamentStatus.OPEN
                )));

        mockMvc.perform(get("/api/v1/tournaments")
                        .with(user("user@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tournamentId").value(tournamentId.toString()))
                .andExpect(jsonPath("$[0].name").value("National Team Friendlies"))
                .andExpect(jsonPath("$[0].slug").value("national-team-friendlies"))
                .andExpect(jsonPath("$[0].seasonYear").value("2026"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        int status = mockMvc.perform(get("/api/v1/tournaments"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(401, 403);
    }
}
