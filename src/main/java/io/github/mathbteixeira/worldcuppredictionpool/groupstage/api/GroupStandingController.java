package io.github.mathbteixeira.worldcuppredictionpool.groupstage.api;

import io.github.mathbteixeira.worldcuppredictionpool.groupstage.application.GroupStandingPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pools/{poolId}/groups")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Group predictions", description = "Group-stage final position prediction APIs.")
public class GroupStandingController {

    private final GroupStandingPredictionService groupStandingPredictionService;

    public GroupStandingController(GroupStandingPredictionService groupStandingPredictionService) {
        this.groupStandingPredictionService = groupStandingPredictionService;
    }

    @GetMapping
    @Operation(summary = "List group predictions", description = "Lists every group in the pool's tournament with its teams, the current user's predicted ordering, and the official standings when confirmed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Groups returned"),
            @ApiResponse(responseCode = "400", description = "Pool is not a tournament pool"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Current user is not a member of the pool"),
            @ApiResponse(responseCode = "404", description = "Pool not found")
    })
    public List<GroupStandingResponse> listGroups(
            @Parameter(description = "Prediction pool id") @PathVariable UUID poolId,
            Authentication authentication) {
        return groupStandingPredictionService.listGroups(poolId, authentication.getName());
    }

    @PutMapping("/{groupName}/prediction")
    @Operation(summary = "Submit or update a group prediction", description = "Creates or replaces the current user's predicted 1st-to-4th ordering for a group, before the group's first kickoff.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Group prediction submitted or updated"),
            @ApiResponse(responseCode = "400", description = "Invalid ordering or pool is not a tournament pool"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Current user is not a member of the pool"),
            @ApiResponse(responseCode = "404", description = "Pool or group not found"),
            @ApiResponse(responseCode = "409", description = "Group predictions are closed")
    })
    public GroupStandingResponse submit(
            @Parameter(description = "Prediction pool id") @PathVariable UUID poolId,
            @Parameter(description = "Group name, for example A") @PathVariable String groupName,
            @Valid @RequestBody SubmitGroupStandingRequest request,
            Authentication authentication) {
        return groupStandingPredictionService.submit(poolId, groupName, authentication.getName(), request.teamIdsByPosition());
    }
}