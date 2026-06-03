export type UserRole = "USER" | "ADMIN";

export type TokenResponse = {
  accessToken: string;
  tokenType: "Bearer";
  username: string;
  email: string;
  role: UserRole;
};

export type ApiErrorResponse = {
  timestamp: string;
  httpStatus: string;
  statusCode: number;
  message: string;
  path: string;
};

export type PoolSummary = {
  id: string;
  tournamentId: string;
  name: string;
  description: string | null;
  inviteCode: string;
  membershipRole: "OWNER" | "MEMBER";
};

export type TeamSummary = {
  id: string;
  name: string;
  fifaCode: string;
};

export type MatchResult = {
  homeScore: number;
  awayScore: number;
  homePenaltyScore: number | null;
  awayPenaltyScore: number | null;
  finalResult: boolean;
};

export type MatchSummary = {
  matchId: string;
  tournamentId: string;
  homeTeam: TeamSummary | null;
  awayTeam: TeamSummary | null;
  homePlaceholder: string | null;
  awayPlaceholder: string | null;
  kickoffAt: string;
  stage: string;
  groupName: string | null;
  status: "SCHEDULED" | "LIVE" | "FINISHED" | "CANCELLED";
  result: MatchResult | null;
  predictionOpen: boolean;
};

export type PredictionResponse = {
  predictionId: string;
  poolId: string;
  matchId: string;
  homeScore: number;
  awayScore: number;
  submittedAt: string;
};

export type PoolPrediction = {
  predictionId: string;
  poolId: string;
  user: {
    userId: string;
    username: string;
  };
  mine: boolean;
  match: MatchSummary;
  homeScore: number;
  awayScore: number;
  submittedAt: string;
};

export type LeaderboardEntry = {
  poolId: string;
  userId: string;
  username: string;
  totalPoints: number;
  rankPosition: number;
  recalculatedAt: string;
};

export type RecalculationResponse = {
  matchId: string;
  homeScore: number;
  awayScore: number;
  homePenaltyScore: number | null;
  awayPenaltyScore: number | null;
  finalResult: boolean;
  resultChecksum: string;
  scoredPredictions: number;
  affectedPools: number;
  idempotentReplay: boolean;
};

export type MatchFilters = {
  group?: string;
  team?: string;
  predictableOnly?: boolean;
};
