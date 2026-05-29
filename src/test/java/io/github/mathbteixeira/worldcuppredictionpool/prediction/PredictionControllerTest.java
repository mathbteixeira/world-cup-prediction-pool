package io.github.mathbteixeira.worldcuppredictionpool.prediction;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.api.PoolPredictionResponse;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.api.PredictionController;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.api.PredictionUserResponse;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.application.PredictionSubmissionService;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.application.SubmitPredictionCommand;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.MatchSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TeamSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserRole;
import io.github.mathbteixeira.worldcuppredictionpool.security.AppUserDetailsService;
import io.github.mathbteixeira.worldcuppredictionpool.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PredictionController.class)
class PredictionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PredictionSubmissionService predictionSubmissionService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    private UUID poolId;
    private UUID matchId;
    private UUID predictionId;
    private Instant submittedAt;
    private Prediction prediction;

    @BeforeEach
    void setUp() {
        poolId = UUID.randomUUID();
        matchId = UUID.randomUUID();
        predictionId = UUID.randomUUID();
        submittedAt = Instant.parse("2026-06-01T10:00:00Z");
        prediction = buildPrediction(poolId, matchId, predictionId, submittedAt, 2, 1);
    }

    @Test
    void shouldReturn200WithPredictionPayloadForAuthenticatedRequest() throws Exception {
        when(predictionSubmissionService.submit(any())).thenReturn(prediction);

        mockMvc.perform(put(endpoint())
                        .with(user("alice@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("homeScore", 2, "awayScore", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictionId").value(predictionId.toString()))
                .andExpect(jsonPath("$.poolId").value(poolId.toString()))
                .andExpect(jsonPath("$.matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.homeScore").value(2))
                .andExpect(jsonPath("$.awayScore").value(1))
                .andExpect(jsonPath("$.submittedAt").value(submittedAt.toString()));
    }

    @Test
    void shouldReturnVisiblePoolPredictionsForAuthenticatedRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        when(predictionSubmissionService.listVisiblePoolPredictions(poolId, "alice@example.com"))
                .thenReturn(List.of(new PoolPredictionResponse(
                        predictionId,
                        poolId,
                        new PredictionUserResponse(userId, "alice"),
                        true,
                        new MatchSummaryResponse(
                                matchId,
                                UUID.randomUUID(),
                                new TeamSummaryResponse(UUID.randomUUID(), "Brazil", "BRA"),
                                new TeamSummaryResponse(UUID.randomUUID(), "Spain", "ESP"),
                                Instant.parse("2026-06-10T10:00:00Z"),
                                "GROUP_STAGE",
                                "A",
                                MatchStatus.SCHEDULED,
                                null,
                                true
                        ),
                        2,
                        1,
                        submittedAt
                )));

        mockMvc.perform(get("/api/v1/pools/" + poolId + "/predictions")
                        .with(user("alice@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].predictionId").value(predictionId.toString()))
                .andExpect(jsonPath("$[0].poolId").value(poolId.toString()))
                .andExpect(jsonPath("$[0].user.userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].user.username").value("alice"))
                .andExpect(jsonPath("$[0].mine").value(true))
                .andExpect(jsonPath("$[0].match.matchId").value(matchId.toString()))
                .andExpect(jsonPath("$[0].match.homeTeam.fifaCode").value("BRA"))
                .andExpect(jsonPath("$[0].match.awayTeam.fifaCode").value("ESP"))
                .andExpect(jsonPath("$[0].match.groupName").value("A"))
                .andExpect(jsonPath("$[0].match.predictionOpen").value(true))
                .andExpect(jsonPath("$[0].homeScore").value(2))
                .andExpect(jsonPath("$[0].awayScore").value(1))
                .andExpect(jsonPath("$[0].submittedAt").value(submittedAt.toString()));
    }

    @Test
    void shouldUseAuthenticatedPrincipalEmailAndIgnoreBodyUserField() throws Exception {
        when(predictionSubmissionService.submit(any())).thenReturn(prediction);

        mockMvc.perform(put(endpoint())
                        .with(user("alice@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "homeScore", 2,
                                "awayScore", 1,
                                "userEmail", "malicious@example.com"
                        ))))
                .andExpect(status().isOk());

        ArgumentCaptor<SubmitPredictionCommand> commandCaptor = ArgumentCaptor.forClass(SubmitPredictionCommand.class);
        verify(predictionSubmissionService).submit(commandCaptor.capture());
        assertThat(commandCaptor.getValue().userEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void shouldReturn400WhenHomeScoreIsMissing() throws Exception {
        mockMvc.perform(put(endpoint())
                        .with(user("alice@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("awayScore", 1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenAwayScoreIsMissing() throws Exception {
        mockMvc.perform(put(endpoint())
                        .with(user("alice@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("homeScore", 2))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenScoreIsNegative() throws Exception {
        mockMvc.perform(put(endpoint())
                        .with(user("alice@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("homeScore", -1, "awayScore", 1))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(endpoint())
                        .with(user("alice@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("homeScore", 1, "awayScore", -1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        int status = mockMvc.perform(put(endpoint())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("homeScore", 2, "awayScore", 1))))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(401, 403);
    }

    @Test
    void shouldReturnConflictErrorPayloadWithMessageWhenServiceThrowsResponseStatusException() throws Exception {
        when(predictionSubmissionService.submit(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Predictions are closed for this match"));

        mockMvc.perform(put(endpoint())
                        .with(user("alice@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("homeScore", 2, "awayScore", 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatus").value("CONFLICT"))
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").value("Predictions are closed for this match"));
    }

    private String endpoint() {
        return "/api/v1/pools/" + poolId + "/matches/" + matchId + "/prediction";
    }

    private static Prediction buildPrediction(UUID poolId, UUID matchId, UUID predictionId, Instant submittedAt, int homeScore, int awayScore) {
        UserAccount user = new UserAccount("alice", "alice@example.com", "hash", UserRole.USER);
        Tournament tournament = new Tournament("World Cup", "wc-2026", "2026", TournamentStatus.OPEN);
        setId(tournament, UUID.randomUUID());
        PredictionPool pool = new PredictionPool("Pool", "desc", "INVITE123", user, tournament);
        setId(pool, poolId);
        Team homeTeam = new Team(tournament, "Brazil", "BRA");
        Team awayTeam = new Team(tournament, "Spain", "ESP");
        Match match = new Match(
                tournament,
                homeTeam,
                awayTeam,
                Instant.parse("2026-06-10T10:00:00Z"),
                "GROUP",
                MatchStatus.SCHEDULED
        );
        setId(match, matchId);
        Prediction prediction = new Prediction(pool, match, user, homeScore, awayScore, submittedAt);
        setId(prediction, predictionId);
        return prediction;
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
