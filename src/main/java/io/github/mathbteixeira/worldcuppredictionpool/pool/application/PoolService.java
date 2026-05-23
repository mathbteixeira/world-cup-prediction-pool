package io.github.mathbteixeira.worldcuppredictionpool.pool.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.api.CreatePoolRequest;
import io.github.mathbteixeira.worldcuppredictionpool.pool.api.PoolSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolRole;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TournamentRepository;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PoolService {

    private final PredictionPoolRepository predictionPoolRepository;
    private final PoolMembershipRepository poolMembershipRepository;
    private final TournamentRepository tournamentRepository;
    private final UserAccountRepository userAccountRepository;

    public PoolService(PredictionPoolRepository predictionPoolRepository,
                       PoolMembershipRepository poolMembershipRepository,
                       TournamentRepository tournamentRepository,
                       UserAccountRepository userAccountRepository) {
        this.predictionPoolRepository = predictionPoolRepository;
        this.poolMembershipRepository = poolMembershipRepository;
        this.tournamentRepository = tournamentRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public PoolSummaryResponse createPool(CreatePoolRequest request, String email) {
        UserAccount owner = getUserByEmail(email);
        Tournament tournament = tournamentRepository.findById(request.tournamentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found"));
        PredictionPool pool = predictionPoolRepository.save(new PredictionPool(
                request.name().trim(),
                request.description() == null ? null : request.description().trim(),
                generateInviteCode(),
                owner,
                tournament
        ));
        poolMembershipRepository.save(new PoolMembership(pool, owner, PoolRole.OWNER));
        return toResponse(pool, PoolRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<PoolSummaryResponse> listPools(String email) {
        return poolMembershipRepository.findAllByUserEmailOrderByCreatedAtDesc(email).stream()
                .map(membership -> toResponse(membership.getPool(), membership.getRole()))
                .toList();
    }

    @Transactional
    public PoolSummaryResponse joinPool(UUID poolId, String inviteCode, String email) {
        PredictionPool pool = predictionPoolRepository.findById(poolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found"));

        if (!pool.getInviteCode().equalsIgnoreCase(inviteCode.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid invite code");
        }

        UserAccount user = getUserByEmail(email);
        if (poolMembershipRepository.findByPoolIdAndUserId(poolId, user.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this pool");
        }

        PoolMembership membership = poolMembershipRepository.save(new PoolMembership(pool, user, PoolRole.MEMBER));
        return toResponse(membership.getPool(), membership.getRole());
    }

    private UserAccount getUserByEmail(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private PoolSummaryResponse toResponse(PredictionPool pool, PoolRole poolRole) {
        return new PoolSummaryResponse(
                pool.getId(),
                pool.getTournament().getId(),
                pool.getName(),
                pool.getDescription(),
                pool.getInviteCode(),
                poolRole.name()
        );
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
