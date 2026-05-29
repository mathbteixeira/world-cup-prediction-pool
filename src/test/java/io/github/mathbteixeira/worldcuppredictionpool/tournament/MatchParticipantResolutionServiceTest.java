package io.github.mathbteixeira.worldcuppredictionpool.tournament;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.MatchSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.application.MatchParticipantResolutionService;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.application.ResolveMatchParticipantsCommand;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchResultRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TeamRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchParticipantResolutionServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    private MatchParticipantResolutionService service;

    private UUID matchId;
    private Tournament tournament;
    private Match match;
    private Team homeTeam;
    private Team awayTeam;

    @BeforeEach
    void setUp() {
        service = new MatchParticipantResolutionService(
                matchRepository,
                teamRepository,
                matchResultRepository,
                Clock.fixed(Instant.parse("2026-06-29T15:00:00Z"), ZoneOffset.UTC)
        );

        tournament = new Tournament("World Cup", "world-cup-2026", "2026", TournamentStatus.OPEN);
        setId(tournament, UUID.randomUUID());
        match = new Match(
                tournament,
                "1A",
                "2B",
                Instant.parse("2026-06-29T16:00:00Z"),
                "ROUND_OF_32",
                MatchStatus.SCHEDULED
        );
        matchId = UUID.randomUUID();
        setId(match, matchId);
        homeTeam = team("Brazil", "BRA", tournament);
        awayTeam = team("Spain", "ESP", tournament);
    }

    @Test
    void shouldResolveParticipantsAndReturnUpdatedMatch() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(teamRepository.findById(homeTeam.getId())).thenReturn(Optional.of(homeTeam));
        when(teamRepository.findById(awayTeam.getId())).thenReturn(Optional.of(awayTeam));
        when(matchRepository.save(match)).thenReturn(match);
        when(matchResultRepository.findByMatchId(matchId)).thenReturn(Optional.empty());

        MatchSummaryResponse response = service.resolve(new ResolveMatchParticipantsCommand(
                matchId,
                homeTeam.getId(),
                awayTeam.getId()
        ));

        assertThat(response.matchId()).isEqualTo(matchId);
        assertThat(response.homeTeam().fifaCode()).isEqualTo("BRA");
        assertThat(response.awayTeam().fifaCode()).isEqualTo("ESP");
        assertThat(response.homePlaceholder()).isNull();
        assertThat(response.awayPlaceholder()).isNull();
        assertThat(response.predictionOpen()).isTrue();
    }

    @Test
    void shouldRejectSameHomeAndAwayTeam() {
        assertThatThrownBy(() -> service.resolve(new ResolveMatchParticipantsCommand(
                matchId,
                homeTeam.getId(),
                homeTeam.getId()
        )))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void shouldRejectTeamsFromDifferentTournament() {
        Tournament otherTournament = new Tournament("Euro", "euro-2028", "2028", TournamentStatus.OPEN);
        setId(otherTournament, UUID.randomUUID());
        Team outsideTeam = team("France", "FRA", otherTournament);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(teamRepository.findById(homeTeam.getId())).thenReturn(Optional.of(homeTeam));
        when(teamRepository.findById(outsideTeam.getId())).thenReturn(Optional.of(outsideTeam));

        assertThatThrownBy(() -> service.resolve(new ResolveMatchParticipantsCommand(
                matchId,
                homeTeam.getId(),
                outsideTeam.getId()
        )))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void shouldRejectAlreadyResolvedMatch() {
        Match resolvedMatch = new Match(
                tournament,
                homeTeam,
                awayTeam,
                Instant.parse("2026-06-29T16:00:00Z"),
                "ROUND_OF_32",
                MatchStatus.SCHEDULED
        );
        setId(resolvedMatch, matchId);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(resolvedMatch));

        assertThatThrownBy(() -> service.resolve(new ResolveMatchParticipantsCommand(
                matchId,
                homeTeam.getId(),
                awayTeam.getId()
        )))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    private Team team(String name, String fifaCode, Tournament tournament) {
        Team team = new Team(tournament, name, fifaCode);
        setId(team, UUID.randomUUID());
        return team;
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
