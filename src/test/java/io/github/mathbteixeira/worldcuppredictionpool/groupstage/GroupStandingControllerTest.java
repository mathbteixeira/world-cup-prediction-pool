package io.github.mathbteixeira.worldcuppredictionpool.groupstage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.api.GroupStandingController;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.api.GroupStandingResponse;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.application.GroupStandingPredictionService;
import io.github.mathbteixeira.worldcuppredictionpool.security.AppUserDetailsService;
import io.github.mathbteixeira.worldcuppredictionpool.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupStandingController.class)
class GroupStandingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GroupStandingPredictionService groupStandingPredictionService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    @Test
    void shouldListGroupsForAuthenticatedMember() throws Exception {
        UUID poolId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        when(groupStandingPredictionService.listGroups(eq(poolId), eq("alice@example.com")))
                .thenReturn(List.of(new GroupStandingResponse(
                        poolId, tournamentId, "A", List.of(), null, true, null, null, false, null)));

        mockMvc.perform(get("/api/v1/pools/" + poolId + "/groups").with(user("alice@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupName").value("A"))
                .andExpect(jsonPath("$[0].predictionOpen").value(true));
    }

    @Test
    void shouldSubmitGroupPredictionWithOrderedTeams() throws Exception {
        UUID poolId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        List<UUID> order = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(groupStandingPredictionService.submit(eq(poolId), eq("A"), eq("alice@example.com"), any()))
                .thenReturn(new GroupStandingResponse(
                        poolId, tournamentId, "A", List.of(), null, true, order, null, false, null));

        mockMvc.perform(put("/api/v1/pools/" + poolId + "/groups/A/prediction")
                        .with(user("alice@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("teamIdsByPosition", order))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupName").value("A"))
                .andExpect(jsonPath("$.predictedTeamIdsByPosition.length()").value(4));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(groupStandingPredictionService).submit(eq(poolId), eq("A"), eq("alice@example.com"), captor.capture());
        assertThat(captor.getValue()).containsExactlyElementsOf(order);
    }

    @Test
    void shouldRejectEmptyOrdering() throws Exception {
        UUID poolId = UUID.randomUUID();
        mockMvc.perform(put("/api/v1/pools/" + poolId + "/groups/A/prediction")
                        .with(user("alice@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("teamIdsByPosition", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        UUID poolId = UUID.randomUUID();
        int status = mockMvc.perform(get("/api/v1/pools/" + poolId + "/groups"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(401, 403);
    }
}