package io.github.mathbteixeira.worldcuppredictionpool.groupstage.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Admin confirmation of a group's official final standings. {@code teamIdsByPosition}
 * lists the team ids from 1st to 4th place and must contain exactly the group's teams.
 */
public record ConfirmGroupStandingRequest(
        @NotNull @NotEmpty List<@NotNull UUID> teamIdsByPosition
) {
}