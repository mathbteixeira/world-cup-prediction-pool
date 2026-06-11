package io.github.mathbteixeira.worldcuppredictionpool.groupstage.api;

import java.util.UUID;

public record GroupTeamResponse(
        UUID id,
        String name,
        String fifaCode
) {
}