package io.github.mathbteixeira.worldcuppredictionpool.tournament;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.MatchSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.application.TournamentMatchService;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchResult;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchResultRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentMatchServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    private TournamentMatchService tournamentMatchService;

    private UUID tournamentId;
    private Tournament tournament;
    private Match mexicoSouthAfrica;
    private Match koreaCzechRepublic;
    private Match canadaBosnia;

    @BeforeEach
    void setUp() {
        tournamentMatchService = new TournamentMatchService(
                tournamentRepository,
                matchRepository,
                matchResultRepository,
                Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC)
        );

        tournamentId = UUID.randomUUID();
        tournament = new Tournament("World Cup", "world-cup-2026", "2026", TournamentStatus.OPEN);
        setId(tournament, tournamentId);

        mexicoSouthAfrica = match(
                "Mexico",
                "MEX",
                "South Africa",
                "RSA",
                Instant.parse("2026-06-11T16:00:00Z"),
                "GROUP_STAGE",
                "A",
                MatchStatus.FINISHED
        );
        koreaCzechRepublic = match(
                "South Korea",
                "KOR",
                "Czech Republic",
                "CZE",
                Instant.parse("2026-06-11T19:00:00Z"),
                "GROUP_STAGE",
                "A",
                MatchStatus.SCHEDULED
        );
        canadaBosnia = match(
                "Canada",
                "CAN",
                "Bosnia Herzegovina",
                "BIH",
                Instant.parse("2026-06-12T16:00:00Z"),
                "GROUP_STAGE",
                "B",
                MatchStatus.SCHEDULED
        );
    }

    @Test
    void shouldReturnMatchesSortedByKickoffWithTeamDataAndPredictionOpen() {
        when(tournamentRepository.existsById(tournamentId)).thenReturn(true);
        when(matchRepository.findAllByTournamentIdOrderByKickoffAtAsc(tournamentId))
                .thenReturn(List.of(mexicoSouthAfrica, koreaCzechRepublic));
        when(matchResultRepository.findAllByMatchIdIn(List.of(mexicoSouthAfrica.getId(), koreaCzechRepublic.getId())))
                .thenReturn(List.of());

        List<MatchSummaryResponse> response = tournamentMatchService.listMatches(
                tournamentId,
                null,
                null,
                null,
                null,
                null,
                false
        );

        assertThat(response).hasSize(2);
        assertThat(response.get(0).matchId()).isEqualTo(mexicoSouthAfrica.getId());
        assertThat(response.get(0).homeTeam().fifaCode()).isEqualTo("MEX");
        assertThat(response.get(0).awayTeam().fifaCode()).isEqualTo("RSA");
        assertThat(response.get(0).predictionOpen()).isFalse();
        assertThat(response.get(1).matchId()).isEqualTo(koreaCzechRepublic.getId());
        assertThat(response.get(1).predictionOpen()).isTrue();
    }

    @Test
    void shouldApplyOptionalFiltersAndPredictableOnly() {
        when(tournamentRepository.existsById(tournamentId)).thenReturn(true);
        when(matchRepository.findAllByTournamentIdOrderByKickoffAtAsc(tournamentId))
                .thenReturn(List.of(mexicoSouthAfrica, koreaCzechRepublic, canadaBosnia));
        when(matchResultRepository.findAllByMatchIdIn(List.of(koreaCzechRepublic.getId())))
                .thenReturn(List.of());

        List<MatchSummaryResponse> response = tournamentMatchService.listMatches(
                tournamentId,
                MatchStatus.SCHEDULED,
                "group_stage",
                "a",
                Instant.parse("2026-06-11T18:00:00Z"),
                Instant.parse("2026-06-11T23:00:00Z"),
                true
        );

        assertThat(response)
                .extracting(MatchSummaryResponse::matchId)
                .containsExactly(koreaCzechRepublic.getId());
    }

    @Test
    void shouldIncludeResultWhenPresent() {
        MatchResult result = new MatchResult(
                mexicoSouthAfrica,
                2,
                1,
                null,
                null,
                true,
                Instant.parse("2026-06-11T18:00:00Z"),
                "checksum"
        );

        when(tournamentRepository.existsById(tournamentId)).thenReturn(true);
        when(matchRepository.findAllByTournamentIdOrderByKickoffAtAsc(tournamentId))
                .thenReturn(List.of(mexicoSouthAfrica));
        when(matchResultRepository.findAllByMatchIdIn(List.of(mexicoSouthAfrica.getId())))
                .thenReturn(List.of(result));

        List<MatchSummaryResponse> response = tournamentMatchService.listMatches(
                tournamentId,
                null,
                null,
                null,
                null,
                null,
                false
        );

        assertThat(response).hasSize(1);
        assertThat(response.get(0).result()).isNotNull();
        assertThat(response.get(0).result().homeScore()).isEqualTo(2);
        assertThat(response.get(0).result().awayScore()).isEqualTo(1);
        assertThat(response.get(0).result().finalResult()).isTrue();
    }

    @Test
    void shouldReturn404ForUnknownTournament() {
        when(tournamentRepository.existsById(tournamentId)).thenReturn(false);

        assertThatThrownBy(() -> tournamentMatchService.listMatches(
                tournamentId,
                null,
                null,
                null,
                null,
                null,
                false
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Tournament not found");
                });

        verify(matchRepository, never()).findAllByTournamentIdOrderByKickoffAtAsc(tournamentId);
    }

    private Match match(String homeName,
                        String homeFifaCode,
                        String awayName,
                        String awayFifaCode,
                        Instant kickoffAt,
                        String stage,
                        String groupName,
                        MatchStatus status) {
        Team home = new Team(tournament, homeName, homeFifaCode);
        Team away = new Team(tournament, awayName, awayFifaCode);
        setId(home, UUID.randomUUID());
        setId(away, UUID.randomUUID());
        Match match = new Match(tournament, home, away, kickoffAt, stage, groupName, status);
        setId(match, UUID.randomUUID());
        return match;
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
