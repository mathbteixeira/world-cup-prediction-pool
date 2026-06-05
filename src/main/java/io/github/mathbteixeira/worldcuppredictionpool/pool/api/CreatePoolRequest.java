package io.github.mathbteixeira.worldcuppredictionpool.pool.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreatePoolRequest(
        @NotBlank @Size(min = 3, max = 120) String name,
        @Size(max = 500) String description,
        PoolMode mode,
        UUID tournamentId,
        UUID matchId,
        @Valid CustomMatchRequest customMatch
) {
    public enum PoolMode {
        TOURNAMENT,
        SINGLE_MATCH
    }

    public record CustomMatchRequest(
            @NotBlank @Size(max = 100) String homeTeam,
            @NotBlank @Size(max = 100) String awayTeam,
            @NotNull
            Instant kickoffAt,
            @Size(max = 60) String competitionLabel
    ) {
    }
}
