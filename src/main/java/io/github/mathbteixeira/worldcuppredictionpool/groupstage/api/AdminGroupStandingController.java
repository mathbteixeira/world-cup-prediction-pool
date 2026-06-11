package io.github.mathbteixeira.worldcuppredictionpool.groupstage.api;

import io.github.mathbteixeira.worldcuppredictionpool.groupstage.application.GroupStandingScoringService;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.application.StandingsRecalculationResult;
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
@RequestMapping("/api/v1/admin/tournaments/{tournamentId}/groups/{groupName}/standings")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin group standings", description = "Administrative confirmation of official group standings and scoring recalculation.")
public class AdminGroupStandingController {

    private final GroupStandingScoringService groupStandingScoringService;

    public AdminGroupStandingController(GroupStandingScoringService groupStandingScoringService) {
        this.groupStandingScoringService = groupStandingScoringService;
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Confirm official group standings", description = "Confirms a group's final 1st-to-4th ordering and recalculates the affected group-position predictions and pool leaderboards. This is the endpoint behind the 'confirm group' admin action.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Standings confirmed and scoring recalculated"),
            @ApiResponse(responseCode = "400", description = "Invalid ordering"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Admin role required"),
            @ApiResponse(responseCode = "404", description = "Tournament or group not found")
    })
    public GroupStandingRecalculationResponse confirmStandings(
            @Parameter(description = "Tournament id") @PathVariable UUID tournamentId,
            @Parameter(description = "Group name, for example A") @PathVariable String groupName,
            @Valid @RequestBody ConfirmGroupStandingRequest request) {
        StandingsRecalculationResult result = groupStandingScoringService.confirmAndRecalculate(
                tournamentId, groupName, request.teamIdsByPosition());
        return new GroupStandingRecalculationResponse(
                tournamentId,
                groupName,
                request.teamIdsByPosition(),
                result.resultChecksum(),
                result.scoredPredictions(),
                result.affectedPools(),
                result.idempotentReplay()
        );
    }
}