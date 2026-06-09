package io.github.mathbteixeira.worldcuppredictionpool.pool.api;

import io.github.mathbteixeira.worldcuppredictionpool.pool.application.AdminPoolModerationService;
import io.github.mathbteixeira.worldcuppredictionpool.pool.application.PoolService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/pools")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin pools", description = "Administrative pool moderation APIs.")
public class AdminPoolController {

    private final AdminPoolModerationService adminPoolModerationService;
    private final PoolService poolService;

    public AdminPoolController(AdminPoolModerationService adminPoolModerationService,
                               PoolService poolService) {
        this.adminPoolModerationService = adminPoolModerationService;
        this.poolService = poolService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminPoolSummaryResponse> listPools() {
        return adminPoolModerationService.listPools();
    }

    @DeleteMapping("/{poolId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePool(@PathVariable UUID poolId) {
        poolService.deletePoolAsAdmin(poolId);
    }

    @GetMapping("/{poolId}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PoolMemberResponse> listMembers(@PathVariable UUID poolId) {
        return adminPoolModerationService.listMembers(poolId);
    }

    @PostMapping("/{poolId}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public PoolMemberResponse addMember(@PathVariable UUID poolId,
                                        @Valid @RequestBody AdminPoolMemberRequest request) {
        return adminPoolModerationService.addMember(poolId, request.email());
    }

    @DeleteMapping("/{poolId}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void removeMember(@PathVariable UUID poolId,
                             @PathVariable UUID userId) {
        adminPoolModerationService.removeMember(poolId, userId);
    }

    @PutMapping("/{poolId}/owner")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminPoolSummaryResponse transferOwnership(@PathVariable UUID poolId,
                                                      @Valid @RequestBody AdminPoolMemberRequest request) {
        return adminPoolModerationService.transferOwnership(poolId, request.email());
    }
}
