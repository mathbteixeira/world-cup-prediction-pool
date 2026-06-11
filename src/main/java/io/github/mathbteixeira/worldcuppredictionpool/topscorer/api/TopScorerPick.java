package io.github.mathbteixeira.worldcuppredictionpool.topscorer.api;

import java.util.UUID;

public record TopScorerPick(
        UUID teamId,
        UUID playerId,
        int goals
) {
}
