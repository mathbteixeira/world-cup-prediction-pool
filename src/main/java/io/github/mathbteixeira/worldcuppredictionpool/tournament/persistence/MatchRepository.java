package io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {
}
