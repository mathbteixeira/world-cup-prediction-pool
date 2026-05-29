package io.github.mathbteixeira.worldcuppredictionpool.tournament.api;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.application.TournamentMatchService;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}/matches")
@SecurityRequirement(name = "bearerAuth")
public class TournamentMatchController {

    private final TournamentMatchService tournamentMatchService;

    public TournamentMatchController(TournamentMatchService tournamentMatchService) {
        this.tournamentMatchService = tournamentMatchService;
    }

    @GetMapping
    public List<MatchSummaryResponse> listMatches(@PathVariable UUID tournamentId,
                                                  @RequestParam(required = false) MatchStatus status,
                                                  @RequestParam(required = false) String stage,
                                                  @RequestParam(name = "group", required = false) String groupName,
                                                  @RequestParam(name = "team", required = false) String teamFifaCode,
                                                  @RequestParam(required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                  @RequestParam(required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                  @RequestParam(defaultValue = "false") boolean predictableOnly) {
        return tournamentMatchService.listMatches(
                tournamentId,
                status,
                stage,
                groupName,
                teamFifaCode,
                from,
                to,
                predictableOnly
        );
    }
}
