package io.github.mathbteixeira.worldcuppredictionpool.topscorer.api;

import java.util.UUID;

public record TopScorerPick(
        UUID teamId,
        String playerName,
        int goals
) {
}
