package io.github.mathbteixeira.worldcuppredictionpool.groupstage.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.groupstage.domain.GroupOfficialStanding;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupOfficialStandingRepository extends JpaRepository<GroupOfficialStanding, UUID> {

    @EntityGraph(attributePaths = {"firstPlaceTeam", "secondPlaceTeam", "thirdPlaceTeam", "fourthPlaceTeam"})
    Optional<GroupOfficialStanding> findByTournamentIdAndGroupName(UUID tournamentId, String groupName);

    @EntityGraph(attributePaths = {"firstPlaceTeam", "secondPlaceTeam", "thirdPlaceTeam", "fourthPlaceTeam"})
    List<GroupOfficialStanding> findAllByTournamentId(UUID tournamentId);
}