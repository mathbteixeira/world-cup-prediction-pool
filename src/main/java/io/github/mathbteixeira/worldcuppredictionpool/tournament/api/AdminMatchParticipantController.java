package io.github.mathbteixeira.worldcuppredictionpool.tournament.api;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.application.MatchParticipantResolutionService;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.application.ResolveMatchParticipantsCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/matches/{matchId}/participants")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin matches", description = "Administrative match lifecycle APIs.")
public class AdminMatchParticipantController {

    private final MatchParticipantResolutionService matchParticipantResolutionService;

    public AdminMatchParticipantController(MatchParticipantResolutionService matchParticipantResolutionService) {
        this.matchParticipantResolutionService = matchParticipantResolutionService;
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update knockout match teams", description = "Assigns or updates real home and away teams for a knockout match.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Knockout match teams updated"),
            @ApiResponse(responseCode = "400", description = "Invalid participant request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Admin role required"),
            @ApiResponse(responseCode = "404", description = "Match or team not found"),
            @ApiResponse(responseCode = "409", description = "Teams do not belong to the match tournament or match is not a knockout match")
    })
    public MatchSummaryResponse resolveParticipants(@Parameter(description = "Match id") @PathVariable UUID matchId,
                                                    @Valid @RequestBody ResolveMatchParticipantsRequest request) {
        return matchParticipantResolutionService.resolve(new ResolveMatchParticipantsCommand(
                matchId,
                request.homeTeamId(),
                request.awayTeamId()
        ));
    }
}
