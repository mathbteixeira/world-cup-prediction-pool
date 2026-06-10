package io.github.mathbteixeira.worldcuppredictionpool.pool.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.api.AdminPoolSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.pool.api.PoolMemberResponse;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolRole;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
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

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AdminPoolModerationService {

    private final PredictionPoolRepository predictionPoolRepository;
    private final PoolMembershipRepository poolMembershipRepository;
    private final UserAccountRepository userAccountRepository;
    private final PredictionRepository predictionRepository;
    private final ScoreEventRepository scoreEventRepository;
    private final PredictionCurrentScoreRepository predictionCurrentScoreRepository;
    private final LeaderboardEntryRepository leaderboardEntryRepository;

    public AdminPoolModerationService(PredictionPoolRepository predictionPoolRepository,
                                      PoolMembershipRepository poolMembershipRepository,
                                      UserAccountRepository userAccountRepository,
                                      PredictionRepository predictionRepository,
                                      ScoreEventRepository scoreEventRepository,
                                      PredictionCurrentScoreRepository predictionCurrentScoreRepository,
                                      LeaderboardEntryRepository leaderboardEntryRepository) {
        this.predictionPoolRepository = predictionPoolRepository;
        this.poolMembershipRepository = poolMembershipRepository;
        this.userAccountRepository = userAccountRepository;
        this.predictionRepository = predictionRepository;
        this.scoreEventRepository = scoreEventRepository;
        this.predictionCurrentScoreRepository = predictionCurrentScoreRepository;
        this.leaderboardEntryRepository = leaderboardEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminPoolSummaryResponse> listPools() {
        return predictionPoolRepository.findAll().stream()
                .sorted(Comparator.comparing(PredictionPool::getName, String.CASE_INSENSITIVE_ORDER))
                .map(pool -> toResponse(pool, poolMembershipRepository.countByPoolId(pool.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PoolMemberResponse> listMembers(UUID poolId) {
        requirePool(poolId);
        return poolMembershipRepository.findAllByPoolId(poolId).stream()
                .sorted(Comparator.comparing(membership -> membership.getUser().getUsername(), String.CASE_INSENSITIVE_ORDER))
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public PoolMemberResponse addMember(UUID poolId, String email) {
        PredictionPool pool = requirePool(poolId);
        UserAccount user = requireUser(email);
        if (poolMembershipRepository.findByPoolIdAndUserId(poolId, user.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this pool");
        }
        PoolMembership membership = poolMembershipRepository.save(new PoolMembership(pool, user, PoolRole.MEMBER));
        return toMemberResponse(membership);
    }

    @Transactional
    public void removeMember(UUID poolId, UUID userId) {
        PredictionPool pool = requirePool(poolId);
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        PoolMembership membership = poolMembershipRepository.findByPoolIdAndUserId(poolId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool member not found"));
        if (membership.getRole() == PoolRole.OWNER || pool.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transfer ownership before removing the pool owner");
        }

        List<UUID> predictionIds = predictionRepository.findIdsByPoolIdAndUserId(poolId, user.getId());
        if (!predictionIds.isEmpty()) {
            scoreEventRepository.deleteByPredictionIdIn(predictionIds);
            predictionCurrentScoreRepository.deleteByPredictionIdIn(predictionIds);
        }
        leaderboardEntryRepository.deleteByPoolIdAndUserId(poolId, user.getId());
        predictionRepository.deleteByPoolIdAndUserId(poolId, user.getId());
        poolMembershipRepository.deleteByPoolIdAndUserId(poolId, user.getId());
    }

    @Transactional
    public AdminPoolSummaryResponse transferOwnership(UUID poolId, String newOwnerEmail) {
        PredictionPool pool = requirePool(poolId);
        UserAccount oldOwner = pool.getOwner();
        UserAccount newOwner = requireUser(newOwnerEmail);

        PoolMembership newOwnerMembership = poolMembershipRepository.findByPoolIdAndUserId(poolId, newOwner.getId())
                .orElseGet(() -> poolMembershipRepository.save(new PoolMembership(pool, newOwner, PoolRole.MEMBER)));
        newOwnerMembership.changeRole(PoolRole.OWNER);

        poolMembershipRepository.findByPoolIdAndUserId(poolId, oldOwner.getId())
                .filter(membership -> !oldOwner.getId().equals(newOwner.getId()))
                .ifPresent(membership -> membership.changeRole(PoolRole.MEMBER));

        pool.transferOwnership(newOwner);
        PredictionPool savedPool = predictionPoolRepository.save(pool);
        return toResponse(savedPool, poolMembershipRepository.countByPoolId(poolId));
    }

    private PredictionPool requirePool(UUID poolId) {
        return predictionPoolRepository.findById(poolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found"));
    }

    private UserAccount requireUser(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private AdminPoolSummaryResponse toResponse(PredictionPool pool, long memberCount) {
        return new AdminPoolSummaryResponse(
                pool.getId(),
                pool.getTournament().getId(),
                pool.getSingleMatch() == null ? null : pool.getSingleMatch().getId(),
                pool.getPoolScope().name(),
                pool.getName(),
                pool.getDescription(),
                pool.getInviteCode(),
                toMemberResponse(pool.getOwner(), PoolRole.OWNER),
                memberCount
        );
    }

    private PoolMemberResponse toMemberResponse(PoolMembership membership) {
        return toMemberResponse(membership.getUser(), membership.getRole());
    }

    private PoolMemberResponse toMemberResponse(UserAccount user, PoolRole role) {
        return new PoolMemberResponse(user.getId(), user.getUsername(), user.getEmail(), role.name());
    }
}
