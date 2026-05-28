package io.github.mathbteixeira.worldcuppredictionpool.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TeamRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MatchRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.8");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Test
    void shouldPersistOptionalGroupName() {
        Tournament tournament = tournamentRepository.save(new Tournament("World Cup", "world-cup-2026", "2026", TournamentStatus.OPEN));
        Team home = teamRepository.save(new Team(tournament, "Brazil", "BRA"));
        Team away = teamRepository.save(new Team(tournament, "Spain", "ESP"));

        Match match = matchRepository.save(new Match(
                tournament,
                home,
                away,
                Instant.parse("2026-06-10T20:00:00Z"),
                "GROUP",
                "A",
                MatchStatus.SCHEDULED
        ));

        assertThat(matchRepository.findById(match.getId()))
                .isPresent()
                .get()
                .extracting(Match::getGroupName)
                .isEqualTo("A");
    }

    @Test
    void shouldAllowMatchesWithoutGroupName() {
        Tournament tournament = tournamentRepository.save(new Tournament("Euro", "euro-2028", "2028", TournamentStatus.OPEN));
        Team home = teamRepository.save(new Team(tournament, "Italy", "ITA"));
        Team away = teamRepository.save(new Team(tournament, "France", "FRA"));

        Match match = matchRepository.save(new Match(
                tournament,
                home,
                away,
                Instant.parse("2028-07-10T20:00:00Z"),
                "FINAL",
                MatchStatus.SCHEDULED
        ));

        assertThat(matchRepository.findById(match.getId()))
                .isPresent()
                .get()
                .extracting(Match::getGroupName)
                .isNull();
    }
}
