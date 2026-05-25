package io.github.mathbteixeira.worldcuppredictionpool.pool.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.api.PoolLeaderboardEntryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.LeaderboardEntryRepository;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class PoolLeaderboardService {

    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final PoolMembershipRepository poolMembershipRepository;
    private final UserAccountRepository userAccountRepository;

    public PoolLeaderboardService(LeaderboardEntryRepository leaderboardEntryRepository,
                                  PoolMembershipRepository poolMembershipRepository,
                                  UserAccountRepository userAccountRepository) {
        this.leaderboardEntryRepository = leaderboardEntryRepository;
        this.poolMembershipRepository = poolMembershipRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public List<PoolLeaderboardEntryResponse> listPoolLeaderboard(UUID poolId, String userEmail) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (poolMembershipRepository.findByPoolIdAndUserId(poolId, user.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this pool");
        }

        return leaderboardEntryRepository.findAllByPoolIdOrderByRankPositionAsc(poolId).stream()
                .map(entry -> new PoolLeaderboardEntryResponse(
                        entry.getPool().getId(),
                        entry.getUser().getId(),
                        entry.getUser().getUsername(),
                        entry.getTotalPoints(),
                        entry.getRankPosition(),
                        entry.getRecalculatedAt()
                ))
                .toList();
    }
}
