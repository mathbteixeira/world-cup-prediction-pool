package io.github.mathbteixeira.worldcuppredictionpool.finalranking;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.api.TournamentRankingController;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.api.TournamentRankingPicks;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.api.TournamentRankingResponse;
import io.github.mathbteixeira.worldcuppredictionpool.finalranking.application.TournamentRankingPredictionService;
import io.github.mathbteixeira.worldcuppredictionpool.security.AppUserDetailsService;
import io.github.mathbteixeira.worldcuppredictionpool.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TournamentRankingController.class)
class TournamentRankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TournamentRankingPredictionService tournamentRankingPredictionService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    @Test
    void shouldReturnFinalRankingView() throws Exception {
        UUID poolId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        when(tournamentRankingPredictionService.getRanking(eq(poolId), eq("alice@example.com")))
                .thenReturn(new TournamentRankingResponse(
                        poolId, tournamentId, List.of(), null, true, null, null, false, null));

        mockMvc.perform(get("/api/v1/pools/" + poolId + "/final-ranking").with(user("alice@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.poolId").value(poolId.toString()))
                .andExpect(jsonPath("$.predictionOpen").value(true));
    }

    @Test
    void shouldSubmitPodium() throws Exception {
        UUID poolId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        UUID champ = UUID.randomUUID();
        UUID runnerUp = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        UUID fourth = UUID.randomUUID();
        when(tournamentRankingPredictionService.submit(eq(poolId), eq("alice@example.com"), any()))
                .thenReturn(new TournamentRankingResponse(
                        poolId, tournamentId, List.of(), null, true,
                        new TournamentRankingPicks(champ, runnerUp, third, fourth), null, false, null));

        Map<String, Object> body = new HashMap<>();
        body.put("championTeamId", champ.toString());
        body.put("runnerUpTeamId", runnerUp.toString());
        body.put("thirdPlaceTeamId", third.toString());
        body.put("fourthPlaceTeamId", fourth.toString());

        mockMvc.perform(put("/api/v1/pools/" + poolId + "/final-ranking/prediction")
                        .with(user("alice@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predicted.championTeamId").value(champ.toString()));
    }

    @Test
    void shouldRejectMissingPodiumFields() throws Exception {
        UUID poolId = UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("championTeamId", UUID.randomUUID().toString());
        // missing the other three fields

        mockMvc.perform(put("/api/v1/pools/" + poolId + "/final-ranking/prediction")
                        .with(user("alice@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        UUID poolId = UUID.randomUUID();
        int status = mockMvc.perform(get("/api/v1/pools/" + poolId + "/final-ranking"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(401, 403);
    }
}