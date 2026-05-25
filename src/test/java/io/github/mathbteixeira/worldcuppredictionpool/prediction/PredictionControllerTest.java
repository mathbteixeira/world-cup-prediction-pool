package io.github.mathbteixeira.worldcuppredictionpool.prediction;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.api.PredictionController;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.api.PredictionResponse;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.api.SubmitPredictionRequest;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.application.PredictionSubmissionService;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.application.SubmitPredictionCommand;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.domain.Prediction;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictionControllerTest {

    @Mock
    private PredictionSubmissionService predictionSubmissionService;

    @InjectMocks
    private PredictionController predictionController;

    @Test
    void shouldUseAuthenticatedEmailAndMapResponse() {
        UUID poolId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        UUID predictionId = UUID.randomUUID();
        Instant submittedAt = Instant.parse("2026-06-01T10:00:00Z");

        UserAccount user = new UserAccount("alice", "alice@example.com", "hash", UserRole.USER);
        Tournament tournament = new Tournament("World Cup", "wc-2026", 2026, TournamentStatus.OPEN);
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
        Prediction prediction = new Prediction(pool, match, user, 2, 1, submittedAt);
        setId(prediction, predictionId);

        when(predictionSubmissionService.submit(org.mockito.ArgumentMatchers.any())).thenReturn(prediction);

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice@example.com", null);
        PredictionResponse response = predictionController.submitOrUpdate(
                poolId,
                matchId,
                new SubmitPredictionRequest(2, 1),
                authentication
        );

        ArgumentCaptor<SubmitPredictionCommand> commandCaptor = ArgumentCaptor.forClass(SubmitPredictionCommand.class);
        verify(predictionSubmissionService).submit(commandCaptor.capture());
        SubmitPredictionCommand command = commandCaptor.getValue();
        assertThat(command.poolId()).isEqualTo(poolId);
        assertThat(command.matchId()).isEqualTo(matchId);
        assertThat(command.userEmail()).isEqualTo("alice@example.com");
        assertThat(command.homeScore()).isEqualTo(2);
        assertThat(command.awayScore()).isEqualTo(1);

        assertThat(response.predictionId()).isEqualTo(predictionId);
        assertThat(response.poolId()).isEqualTo(poolId);
        assertThat(response.matchId()).isEqualTo(matchId);
        assertThat(response.homeScore()).isEqualTo(2);
        assertThat(response.awayScore()).isEqualTo(1);
        assertThat(response.submittedAt()).isEqualTo(submittedAt);
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
