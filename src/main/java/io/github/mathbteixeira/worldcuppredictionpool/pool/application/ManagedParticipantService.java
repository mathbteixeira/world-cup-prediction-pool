package io.github.mathbteixeira.worldcuppredictionpool.pool.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.api.ManagedParticipantResponse;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.ManagedParticipant;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolRole;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.ManagedParticipantRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence.PredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.LeaderboardEntryRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.PredictionCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.ScoreEventRepository;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ManagedParticipantService {

    private final ManagedParticipantRepository managedParticipantRepository;
    private final PredictionPoolRepository predictionPoolRepository;
    private final PoolMembershipRepository poolMembershipRepository;
    private final PredictionRepository predictionRepository;
    private final ScoreEventRepository scoreEventRepository;
    private final PredictionCurrentScoreRepository predictionCurrentScoreRepository;
    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final UserAccountRepository userAccountRepository;

    public ManagedParticipantService(ManagedParticipantRepository managedParticipantRepository,
                                     PredictionPoolRepository predictionPoolRepository,
                                     PoolMembershipRepository poolMembershipRepository,
                                     PredictionRepository predictionRepository,
                                     ScoreEventRepository scoreEventRepository,
                                     PredictionCurrentScoreRepository predictionCurrentScoreRepository,
                                     LeaderboardEntryRepository leaderboardEntryRepository,
                                     UserAccountRepository userAccountRepository) {
        this.managedParticipantRepository = managedParticipantRepository;
        this.predictionPoolRepository = predictionPoolRepository;
        this.poolMembershipRepository = poolMembershipRepository;
        this.predictionRepository = predictionRepository;
        this.scoreEventRepository = scoreEventRepository;
        this.predictionCurrentScoreRepository = predictionCurrentScoreRepository;
        this.leaderboardEntryRepository = leaderboardEntryRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public ManagedParticipantResponse create(UUID poolId, String name, String ownerEmail) {
        PredictionPool pool = requireOwnerManagedSingleMatchPool(poolId, ownerEmail);
        String displayName = name.trim();
        if (managedParticipantRepository.existsByPoolIdAndDisplayNameIgnoreCase(poolId, displayName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Managed participant name already exists in this pool");
        }
        return toResponse(managedParticipantRepository.save(new ManagedParticipant(pool, displayName)));
    }

    @Transactional(readOnly = true)
    public List<ManagedParticipantResponse> list(UUID poolId, String ownerEmail) {
        requireOwnerManagedSingleMatchPool(poolId, ownerEmail);
        return managedParticipantRepository.findAllByPoolIdOrderByDisplayNameAsc(poolId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void remove(UUID poolId, UUID participantId, String ownerEmail) {
        requireOwnerManagedSingleMatchPool(poolId, ownerEmail);
        ManagedParticipant participant = managedParticipantRepository.findByPoolIdAndId(poolId, participantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Managed participant not found"));
        List<UUID> predictionIds = predictionRepository.findIdsByManagedParticipantId(participantId);
        if (!predictionIds.isEmpty()) {
            scoreEventRepository.deleteByPredictionIdIn(predictionIds);
            predictionCurrentScoreRepository.deleteByPredictionIdIn(predictionIds);
            predictionRepository.deleteByManagedParticipantId(participantId);
        }
        leaderboardEntryRepository.deleteByManagedParticipantId(participantId);
        managedParticipantRepository.delete(participant);
    }

    private PredictionPool requireOwnerManagedSingleMatchPool(UUID poolId, String ownerEmail) {
        PredictionPool pool = predictionPoolRepository.findById(poolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found"));
        if (!pool.isSingleMatchPool()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Managed participants are supported only for single-match pools");
        }

        UserAccount user = userAccountRepository.findByEmailIgnoreCase(ownerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return poolMembershipRepository.findByPoolIdAndUserId(poolId, user.getId())
                .filter(membership -> membership.getRole() == PoolRole.OWNER)
                .map(ignored -> pool)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the pool owner can manage participants"));
    }

    private ManagedParticipantResponse toResponse(ManagedParticipant participant) {
        return new ManagedParticipantResponse(
                participant.getId(),
                participant.getPool().getId(),
                participant.getDisplayName()
        );
    }
}
