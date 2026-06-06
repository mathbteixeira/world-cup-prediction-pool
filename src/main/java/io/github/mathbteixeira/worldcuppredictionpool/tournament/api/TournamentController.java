package io.github.mathbteixeira.worldcuppredictionpool.tournament.api;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.application.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tournaments")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tournaments", description = "Tournament discovery APIs.")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @GetMapping
    @Operation(summary = "List tournaments", description = "Lists tournaments that can be used to discover matches.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tournaments returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public List<TournamentSummaryResponse> listTournaments() {
        return tournamentService.listTournaments();
    }
}
