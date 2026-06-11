package io.github.mathbteixeira.worldcuppredictionpool.groupstage.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * A user's predicted group ordering. {@code teamIdsByPosition} lists the team
 * ids from 1st to 4th place; it must contain exactly the group's teams, each
 * once.
 */
public record SubmitGroupStandingRequest(
        @NotNull @NotEmpty List<@NotNull UUID> teamIdsByPosition
) {
}