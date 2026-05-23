package io.github.mathbteixeira.worldcuppredictionpool.pool.api;

import io.github.mathbteixeira.worldcuppredictionpool.pool.application.PoolService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
public class PoolController {

    private final PoolService poolService;

    public PoolController(PoolService poolService) {
        this.poolService = poolService;
    }

    @PostMapping
    public PoolSummaryResponse create(@Valid @RequestBody CreatePoolRequest request, Authentication authentication) {
        return poolService.createPool(request, authentication.getName());
    }

    @GetMapping
    public List<PoolSummaryResponse> list(Authentication authentication) {
        return poolService.listPools(authentication.getName());
    }

    @PostMapping("/{poolId}/join")
    public PoolSummaryResponse join(@PathVariable UUID poolId,
                                    @Valid @RequestBody JoinPoolRequest request,
                                    Authentication authentication) {
        return poolService.joinPool(poolId, request.inviteCode(), authentication.getName());
    }
}
