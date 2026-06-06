package io.github.mathbteixeira.worldcuppredictionpool.tournament.application;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.api.TournamentSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;

    public TournamentService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    @Transactional(readOnly = true)
    public List<TournamentSummaryResponse> listTournaments() {
        return tournamentRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private TournamentSummaryResponse toResponse(Tournament tournament) {
        return new TournamentSummaryResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getSlug(),
                tournament.getSeasonYear(),
                tournament.getStatus()
        );
    }
}
