package io.github.mathbteixeira.worldcuppredictionpool.scoring.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpsertMatchResultRequest(
        @NotNull @Min(0) Integer homeScore,
        @NotNull @Min(0) Integer awayScore,
        @Min(0) Integer homePenaltyScore,
        @Min(0) Integer awayPenaltyScore,
        @NotNull Boolean finalResult,
        UUID matchId
) {
}
