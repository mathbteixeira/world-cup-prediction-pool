package io.github.mathbteixeira.worldcuppredictionpool.pool.api;

import io.github.mathbteixeira.worldcuppredictionpool.pool.application.PoolService;
import io.github.mathbteixeira.worldcuppredictionpool.pool.application.PoolLeaderboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pools")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pools", description = "Prediction pool creation, membership, and leaderboard APIs.")
public class PoolController {

    private final PoolService poolService;
    private final PoolLeaderboardService poolLeaderboardService;

    public PoolController(PoolService poolService, PoolLeaderboardService poolLeaderboardService) {
        this.poolService = poolService;
        this.poolLeaderboardService = poolLeaderboardService;
    }

    @PostMapping
    @Operation(summary = "Create pool", description = "Creates either a tournament pool or a single-match pool and makes the current user the owner.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pool created"),
            @ApiResponse(responseCode = "400", description = "Invalid pool request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Tournament or match not found")
    })
    public PoolSummaryResponse create(@Valid @RequestBody CreatePoolRequest request, Authentication authentication) {
        return poolService.createPool(request, authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List my pools", description = "Lists prediction pools where the current user is a member.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pools returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public List<PoolSummaryResponse> list(Authentication authentication) {
        return poolService.listPools(authentication.getName());
    }

    @GetMapping("/{poolId}")
    @Operation(summary = "Get pool", description = "Returns a prediction pool where the current user is a member.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pool returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Current user is not a member of the pool")
    })
    public PoolSummaryResponse get(@Parameter(description = "Prediction pool id") @PathVariable UUID poolId,
                                   Authentication authentication) {
        return poolService.getPool(poolId, authentication.getName());
    }

    @GetMapping("/{poolId}/leaderboard")
    @Operation(summary = "Get pool leaderboard", description = "Returns the current leaderboard for a prediction pool.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leaderboard returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Current user is not a member of the pool"),
            @ApiResponse(responseCode = "404", description = "Pool not found")
    })
    public List<PoolLeaderboardEntryResponse> leaderboard(
            @Parameter(description = "Prediction pool id") @PathVariable UUID poolId,
            Authentication authentication) {
        return poolLeaderboardService.listPoolLeaderboard(poolId, authentication.getName());
    }

    @PostMapping("/{poolId}/join")
    @Operation(summary = "Join pool", description = "Adds the current user to a prediction pool using its invite code.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pool joined"),
            @ApiResponse(responseCode = "400", description = "Invalid invite code"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Pool not found")
    })
    public PoolSummaryResponse join(@Parameter(description = "Prediction pool id") @PathVariable UUID poolId,
                                    @Valid @RequestBody JoinPoolRequest request,
                                    Authentication authentication) {
        return poolService.joinPool(poolId, request.inviteCode(), authentication.getName());
    }

    @PostMapping("/join")
    @Operation(summary = "Join pool by invite code", description = "Adds the current user to a prediction pool using only its invite code.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pool joined"),
            @ApiResponse(responseCode = "400", description = "Invalid invite code"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Pool not found")
    })
    public PoolSummaryResponse joinByInviteCode(@Valid @RequestBody JoinPoolRequest request,
                                                Authentication authentication) {
        return poolService.joinPoolByInviteCode(request.inviteCode(), authentication.getName());
    }
}
