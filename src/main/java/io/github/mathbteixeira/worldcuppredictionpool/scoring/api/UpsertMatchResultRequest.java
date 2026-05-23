package io.github.mathbteixeira.worldcuppredictionpool.scoring.api;

import jakarta.validation.constraints.Min;

public record UpsertMatchResultRequest(
        @Min(0) int homeScore,
        @Min(0) int awayScore,
        @Min(0) Integer homePenaltyScore,
        @Min(0) Integer awayPenaltyScore,
        boolean finalResult
) {
}
