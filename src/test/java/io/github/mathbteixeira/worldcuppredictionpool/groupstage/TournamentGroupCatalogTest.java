package io.github.mathbteixeira.worldcuppredictionpool.groupstage;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.groupstage.application.TournamentGroupCatalog;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentGroupCatalogTest {

    @Mock
    private MatchRepository matchRepository;

    @Test
    void groupAUsesJuneTwelveMidnightPredictionDeadline() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = new Tournament("World Cup", "world-cup-2026", "2026", TournamentStatus.OPEN);
        Team mexico = team(tournament, "Mexico", "MEX");
        Team southAfrica = team(tournament, "South Africa", "RSA");
        Team canada = team(tournament, "Canada", "CAN");
        Team qatar = team(tournament, "Qatar", "QAT");
        when(matchRepository.findAllByTournamentIdOrderByKickoffAtAsc(tournamentId)).thenReturn(List.of(
                groupMatch(tournament, mexico, southAfrica, "A", "2026-06-11T19:00:00Z"),
                groupMatch(tournament, canada, qatar, "B", "2026-06-12T19:00:00Z")
        ));

        Map<String, TournamentGroupCatalog.GroupInfo> groups = new TournamentGroupCatalog(matchRepository)
                .groupsOf(tournamentId);

        assertThat(groups.get("A").earliestKickoff()).isEqualTo(Instant.parse("2026-06-12T03:00:00Z"));
        assertThat(groups.get("A").predictionsOpenAt(Instant.parse("2026-06-12T02:59:59Z"))).isTrue();
        assertThat(groups.get("A").predictionsOpenAt(Instant.parse("2026-06-12T03:00:00Z"))).isFalse();
        assertThat(groups.get("B").earliestKickoff()).isEqualTo(Instant.parse("2026-06-12T19:00:00Z"));
    }

    private static Match groupMatch(Tournament tournament, Team home, Team away, String groupName, String kickoffAt) {
        return new Match(tournament, home, away, Instant.parse(kickoffAt), "GROUP_STAGE", groupName, MatchStatus.SCHEDULED);
    }

    private static Team team(Tournament tournament, String name, String fifaCode) {
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
