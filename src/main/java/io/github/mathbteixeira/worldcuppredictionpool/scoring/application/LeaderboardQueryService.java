package io.github.mathbteixeira.worldcuppredictionpool.scoring.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.domain.LeaderboardEntry;
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
public class LeaderboardQueryService {

    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final UserAccountRepository userAccountRepository;
    private final PoolMembershipRepository poolMembershipRepository;

    public LeaderboardQueryService(LeaderboardEntryRepository leaderboardEntryRepository,
                                   UserAccountRepository userAccountRepository,
                                   PoolMembershipRepository poolMembershipRepository) {
        this.leaderboardEntryRepository = leaderboardEntryRepository;
        this.userAccountRepository = userAccountRepository;
        this.poolMembershipRepository = poolMembershipRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getByPool(UUID poolId, String requesterEmail) {
        UserAccount requester = userAccountRepository.findByEmailIgnoreCase(requesterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (poolMembershipRepository.findByPoolIdAndUserId(poolId, requester.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this pool");
        }
        return leaderboardEntryRepository.findAllByPoolIdOrderByRankPositionAsc(poolId);
    }
}
