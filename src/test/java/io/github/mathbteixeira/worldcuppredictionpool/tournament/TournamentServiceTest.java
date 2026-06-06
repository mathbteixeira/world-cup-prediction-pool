package io.github.mathbteixeira.worldcuppredictionpool.tournament;

import io.github.mathbteixeira.worldcuppredictionpool.common.model.BaseEntity;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TournamentSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.application.TournamentService;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Test
    void shouldListTournamentsInRepositoryOrder() {
        Tournament friendlies = tournament("National Team Friendlies", "national-team-friendlies", "2026");
        Tournament worldCup = tournament("World Cup", "world-cup", "2026");
        when(tournamentRepository.findAllByOrderByNameAsc()).thenReturn(List.of(friendlies, worldCup));

        TournamentService tournamentService = new TournamentService(tournamentRepository);

        List<TournamentSummaryResponse> response = tournamentService.listTournaments();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).tournamentId()).isEqualTo(friendlies.getId());
        assertThat(response.get(0).name()).isEqualTo("National Team Friendlies");
        assertThat(response.get(0).slug()).isEqualTo("national-team-friendlies");
        assertThat(response.get(0).seasonYear()).isEqualTo("2026");
        assertThat(response.get(0).status()).isEqualTo(TournamentStatus.OPEN);
        assertThat(response.get(1).tournamentId()).isEqualTo(worldCup.getId());
    }

    private Tournament tournament(String name, String slug, String seasonYear) {
        Tournament tournament = new Tournament(name, slug, seasonYear, TournamentStatus.OPEN);
        setId(tournament, UUID.randomUUID());
        return tournament;
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
