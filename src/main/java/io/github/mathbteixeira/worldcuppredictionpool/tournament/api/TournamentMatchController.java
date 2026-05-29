package io.github.mathbteixeira.worldcuppredictionpool.tournament.api;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.application.TournamentMatchService;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Tournaments", description = "Tournament match discovery APIs.")
public class TournamentMatchController {

    private final TournamentMatchService tournamentMatchService;

    public TournamentMatchController(TournamentMatchService tournamentMatchService) {
        this.tournamentMatchService = tournamentMatchService;
    }

    @GetMapping
    @Operation(summary = "List tournament matches", description = "Lists matches for a tournament so users can discover match ids before submitting predictions. Results are sorted by kickoff time ascending.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matches returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public List<MatchSummaryResponse> listMatches(
                                                  @Parameter(description = "Tournament id") @PathVariable UUID tournamentId,
                                                  @Parameter(description = "Optional match status filter") @RequestParam(required = false) MatchStatus status,
                                                  @Parameter(description = "Optional stage filter, for example GROUP_STAGE") @RequestParam(required = false) String stage,
                                                  @Parameter(description = "Optional group filter, for example A") @RequestParam(name = "group", required = false) String groupName,
                                                  @Parameter(description = "Optional FIFA code filter. Matches either home or away team, case-insensitive.") @RequestParam(name = "team", required = false) String teamFifaCode,
                                                  @Parameter(description = "Optional inclusive kickoff lower bound")
                                                  @RequestParam(required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                  @Parameter(description = "Optional inclusive kickoff upper bound")
                                                  @RequestParam(required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                  @Parameter(description = "When true, only returns matches still open for predictions")
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
