package io.github.mathbteixeira.worldcuppredictionpool.pool.application;

import io.github.mathbteixeira.worldcuppredictionpool.pool.api.CreatePoolRequest;
import io.github.mathbteixeira.worldcuppredictionpool.pool.api.PoolSummaryResponse;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolMembership;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PoolRole;
import io.github.mathbteixeira.worldcuppredictionpool.pool.domain.PredictionPool;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.ManagedParticipantRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PoolMembershipRepository;
import io.github.mathbteixeira.worldcuppredictionpool.pool.persistence.PredictionPoolRepository;
import io.github.mathbteixeira.worldcuppredictionpool.prediction.persistence.PredictionRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.LeaderboardEntryRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.PredictionCurrentScoreRepository;
import io.github.mathbteixeira.worldcuppredictionpool.scoring.persistence.ScoreEventRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Match;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.MatchStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Team;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.Tournament;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.domain.TournamentStatus;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.MatchRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TeamRepository;
import io.github.mathbteixeira.worldcuppredictionpool.tournament.persistence.TournamentRepository;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserRole;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PoolService {

    private final PredictionPoolRepository predictionPoolRepository;
    private final PoolMembershipRepository poolMembershipRepository;
    private final ManagedParticipantRepository managedParticipantRepository;
    private final PredictionRepository predictionRepository;
    private final ScoreEventRepository scoreEventRepository;
    private final PredictionCurrentScoreRepository predictionCurrentScoreRepository;
    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final UserAccountRepository userAccountRepository;

    public PoolService(PredictionPoolRepository predictionPoolRepository,
                       PoolMembershipRepository poolMembershipRepository,
                       ManagedParticipantRepository managedParticipantRepository,
                       PredictionRepository predictionRepository,
                       ScoreEventRepository scoreEventRepository,
                       PredictionCurrentScoreRepository predictionCurrentScoreRepository,
                       LeaderboardEntryRepository leaderboardEntryRepository,
                       TournamentRepository tournamentRepository,
                       MatchRepository matchRepository,
                       TeamRepository teamRepository,
                       UserAccountRepository userAccountRepository) {
        this.predictionPoolRepository = predictionPoolRepository;
        this.poolMembershipRepository = poolMembershipRepository;
        this.managedParticipantRepository = managedParticipantRepository;
        this.predictionRepository = predictionRepository;
        this.scoreEventRepository = scoreEventRepository;
        this.predictionCurrentScoreRepository = predictionCurrentScoreRepository;
        this.leaderboardEntryRepository = leaderboardEntryRepository;
        this.tournamentRepository = tournamentRepository;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public PoolSummaryResponse createPool(CreatePoolRequest request, String email) {
        UserAccount owner = getUserByEmail(email);
        CreatePoolRequest.PoolMode mode = request.mode() == null ? CreatePoolRequest.PoolMode.TOURNAMENT : request.mode();
        if (mode == CreatePoolRequest.PoolMode.SINGLE_MATCH) {
            return createSingleMatchPool(request, owner);
        }

        if (request.tournamentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tournament pool must reference a tournament");
        }
        if (request.matchId() != null || request.customMatch() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tournament pool cannot reference a single match");
        }

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

    private PoolSummaryResponse createSingleMatchPool(CreatePoolRequest request, UserAccount owner) {
        boolean hasExistingMatch = request.matchId() != null;
        boolean hasCustomMatch = request.customMatch() != null;
        if (hasExistingMatch == hasCustomMatch) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Single-match pool must reference exactly one existing or custom match");
        }
        if (request.tournamentId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Single-match pool derives its tournament from the selected match");
        }

        Match match = hasExistingMatch
                ? matchRepository.findById(request.matchId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"))
                : createCustomMatch(request.customMatch());

        PredictionPool pool = predictionPoolRepository.save(new PredictionPool(
                request.name().trim(),
                request.description() == null ? null : request.description().trim(),
                generateInviteCode(),
                owner,
                match
        ));
        poolMembershipRepository.save(new PoolMembership(pool, owner, PoolRole.OWNER));
        return toResponse(pool, PoolRole.OWNER);
    }

    private Match createCustomMatch(CreatePoolRequest.CustomMatchRequest request) {
        if (request.kickoffAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Custom match kickoff time is required");
        }

        String competitionLabel = request.competitionLabel() == null || request.competitionLabel().isBlank()
                ? "Custom Match"
                : request.competitionLabel().trim();
        String slug = "single-match-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String seasonYear = String.valueOf(request.kickoffAt().atZone(ZoneOffset.UTC).getYear());
        Tournament tournament = tournamentRepository.save(new Tournament(competitionLabel, slug, seasonYear, TournamentStatus.OPEN));

        String homeName = request.homeTeam().trim();
        String awayName = request.awayTeam().trim();
        if (homeName.equalsIgnoreCase(awayName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Custom match teams must be different");
        }
        String homeFifaCode = fifaCodeFor(homeName, "HME");
        String awayFifaCode = fifaCodeFor(awayName, "AWY");
        if (homeFifaCode.equals(awayFifaCode)) {
            awayFifaCode = homeFifaCode.equals("AWY") ? "AW2" : "AWY";
        }
        Team home = teamRepository.save(new Team(tournament, homeName, homeFifaCode));
        Team away = teamRepository.save(new Team(tournament, awayName, awayFifaCode));

        return matchRepository.save(new Match(
                tournament,
                home,
                away,
                request.kickoffAt(),
                competitionLabel,
                MatchStatus.SCHEDULED
        ));
    }

    @Transactional(readOnly = true)
    public List<PoolSummaryResponse> listPools(String email) {
        return poolMembershipRepository.findAllByUserEmailOrderByCreatedAtDesc(email).stream()
                .map(membership -> toResponse(membership.getPool(), membership.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PoolSummaryResponse getPool(UUID poolId, String email) {
        predictionPoolRepository.findById(poolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found"));
        UserAccount user = getUserByEmail(email);
        PoolMembership membership = poolMembershipRepository.findByPoolIdAndUserId(poolId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a member of this pool"));
        return toResponse(membership.getPool(), membership.getRole());
    }

    @Transactional
    public PoolSummaryResponse joinPoolByInviteCode(String inviteCode, String email) {
        PredictionPool pool = predictionPoolRepository.findByInviteCodeIgnoreCase(inviteCode.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found"));
        return joinPool(pool.getId(), inviteCode, email);
    }

    @Transactional
    public PoolSummaryResponse joinPool(UUID poolId, String inviteCode, String email) {
        PredictionPool pool = predictionPoolRepository.findById(poolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found"));

        UserAccount user = getUserByEmail(email);
        if (poolMembershipRepository.findByPoolIdAndUserId(poolId, user.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this pool");
        }

        if (!pool.getInviteCode().equalsIgnoreCase(inviteCode.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid invite code");
        }

        PoolMembership membership = poolMembershipRepository.save(new PoolMembership(pool, user, PoolRole.MEMBER));
        return toResponse(membership.getPool(), membership.getRole());
    }

    @Transactional
    public void deletePool(UUID poolId, String email) {
        PredictionPool pool = predictionPoolRepository.findById(poolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found"));
        UserAccount user = getUserByEmail(email);
        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        boolean isOwner = pool.getOwner().getId().equals(user.getId());
        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the pool owner or an admin can delete this pool");
        }

        deletePoolData(poolId, pool);
    }

    @Transactional
    public void deletePoolAsAdmin(UUID poolId) {
        PredictionPool pool = predictionPoolRepository.findById(poolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pool not found"));
        deletePoolData(poolId, pool);
    }

    private void deletePoolData(UUID poolId, PredictionPool pool) {
        leaderboardEntryRepository.deleteByPoolId(poolId);
        scoreEventRepository.deleteByPoolId(poolId);
        predictionCurrentScoreRepository.deleteByPoolId(poolId);
        predictionRepository.deleteByPoolId(poolId);
        managedParticipantRepository.deleteByPoolId(poolId);
        poolMembershipRepository.deleteByPoolId(poolId);
        predictionPoolRepository.delete(pool);
    }

    private UserAccount getUserByEmail(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private PoolSummaryResponse toResponse(PredictionPool pool, PoolRole poolRole) {
        return new PoolSummaryResponse(
                pool.getId(),
                pool.getTournament().getId(),
                pool.getSingleMatch() == null ? null : pool.getSingleMatch().getId(),
                pool.getPoolScope().name(),
                pool.getName(),
                pool.getDescription(),
                pool.getInviteCode(),
                poolRole.name()
        );
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String fifaCodeFor(String teamName, String fallback) {
        String code = teamName.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (code.isBlank()) {
            return fallback;
        }
        return (code + "XXX").substring(0, 3);
    }
}
