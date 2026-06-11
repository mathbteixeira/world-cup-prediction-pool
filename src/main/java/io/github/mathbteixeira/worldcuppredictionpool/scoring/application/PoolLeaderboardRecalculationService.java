package io.github.mathbteixeira.worldcuppredictionpool.scoring.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.ManagedParticipant;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.ManagedParticipantRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.domain.LeaderboardEntry;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.LeaderboardEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rebuilds pool leaderboards deterministically from every point source.
 *
 * <p>This is the single place where leaderboard totals are assembled. It is
 * shared by all scoring triggers (match results, group-position confirmation,
 * and tournament final-ranking confirmation) so a rebuild caused by one
 * prediction type never drops the points earned from another. Totals are
 * derived from the contributor projections inside one transaction, which keeps
 * the rebuild idempotent: running it repeatedly with unchanged inputs yields the
 * same leaderboard.
 */
@Service
public class PoolLeaderboardRecalculationService {

    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final PoolMembershipRepository poolMembershipRepository;
    private final ManagedParticipantRepository managedParticipantRepository;
    private final List<LeaderboardPointContributor> pointContributors;

    public PoolLeaderboardRecalculationService(LeaderboardEntryRepository leaderboardEntryRepository,
                                               PoolMembershipRepository poolMembershipRepository,
                                               ManagedParticipantRepository managedParticipantRepository,
                                               List<LeaderboardPointContributor> pointContributors) {
        this.leaderboardEntryRepository = leaderboardEntryRepository;
        this.poolMembershipRepository = poolMembershipRepository;
        this.managedParticipantRepository = managedParticipantRepository;
        this.pointContributors = pointContributors;
    }

    @Transactional
    public void rebuild(List<UUID> poolIds, Instant now) {
        if (poolIds == null || poolIds.isEmpty()) {
            return;
        }

        leaderboardEntryRepository.deleteByPoolIdIn(poolIds);

        List<PoolMembership> memberships = poolMembershipRepository.findAllByPoolIdIn(poolIds);
        List<ManagedParticipant> managedParticipants = managedParticipantRepository.findAllByPoolIdIn(poolIds);

        Map<UUID, PredictionPool> poolsById = new LinkedHashMap<>();
        memberships.forEach(membership -> poolsById.putIfAbsent(membership.getPool().getId(), membership.getPool()));
        managedParticipants.forEach(participant -> poolsById.putIfAbsent(participant.getPool().getId(), participant.getPool()));

        Map<UUID, PoolMembership> membershipsByUserId = new LinkedHashMap<>();
        memberships.forEach(membership -> membershipsByUserId.putIfAbsent(membership.getUser().getId(), membership));
        Map<UUID, ManagedParticipant> managedParticipantsById = new LinkedHashMap<>();
        managedParticipants.forEach(participant -> managedParticipantsById.put(participant.getId(), participant));

        // Seed every known participant at zero so members without points still rank.
        Map<UUID, Map<ParticipantKey, Long>> totalsByPoolAndParticipant = new LinkedHashMap<>();
        for (PoolMembership membership : memberships) {
            totalsByPoolAndParticipant
                    .computeIfAbsent(membership.getPool().getId(), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(ParticipantKey.user(membership.getUser().getId()), 0L);
        }
        for (ManagedParticipant participant : managedParticipants) {
            totalsByPoolAndParticipant
                    .computeIfAbsent(participant.getPool().getId(), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(ParticipantKey.managed(participant.getId()), 0L);
        }

        // Accumulate points from every prediction type that contributes to the leaderboard.
        for (LeaderboardPointContributor contributor : pointContributors) {
            for (ParticipantPoints contribution : contributor.contributionsFor(poolIds)) {
                ParticipantKey key = contribution.userId() != null
                        ? ParticipantKey.user(contribution.userId())
                        : ParticipantKey.managed(contribution.managedParticipantId());
                totalsByPoolAndParticipant
                        .computeIfAbsent(contribution.poolId(), ignored -> new LinkedHashMap<>())
                        .merge(key, contribution.points(), Long::sum);
            }
        }

        List<LeaderboardEntry> rebuilt = new ArrayList<>();
        for (Map.Entry<UUID, Map<ParticipantKey, Long>> poolEntry : totalsByPoolAndParticipant.entrySet()) {
            PredictionPool poolReference = poolsById.get(poolEntry.getKey());
            if (poolReference == null) {
                continue;
            }

            List<Map.Entry<ParticipantKey, Long>> ranking = poolEntry.getValue().entrySet().stream()
                    .sorted(Comparator
                            .comparing(Map.Entry<ParticipantKey, Long>::getValue, Comparator.reverseOrder())
                            .thenComparing(entry -> entry.getKey().sortValue()))
                    .toList();

            int rank = 1;
            Long previousTotal = null;
            for (int index = 0; index < ranking.size(); index++) {
                Map.Entry<ParticipantKey, Long> participantTotal = ranking.get(index);
                if (previousTotal != null && !previousTotal.equals(participantTotal.getValue())) {
                    rank = index + 1;
                }
                previousTotal = participantTotal.getValue();

                ParticipantKey participantKey = participantTotal.getKey();
                int totalPoints = Math.toIntExact(participantTotal.getValue());
                if (participantKey.managed()) {
                    rebuilt.add(new LeaderboardEntry(
                            poolReference,
                            managedParticipantsById.get(participantKey.id()),
                            totalPoints,
                            rank,
                            now
                    ));
                } else {
                    rebuilt.add(new LeaderboardEntry(
                            poolReference,
                            membershipsByUserId.get(participantKey.id()).getUser(),
                            totalPoints,
                            rank,
                            now
                    ));
                }
            }
        }

        if (!rebuilt.isEmpty()) {
            leaderboardEntryRepository.saveAll(rebuilt);
        }
    }

    private record ParticipantKey(UUID id, boolean managed) {
        static ParticipantKey user(UUID id) {
            return new ParticipantKey(id, false);
        }

        static ParticipantKey managed(UUID id) {
            return new ParticipantKey(id, true);
        }

        String sortValue() {
            return (managed ? "managed:" : "user:") + id;
        }
    }
}