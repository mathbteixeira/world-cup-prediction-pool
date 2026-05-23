package io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
}
