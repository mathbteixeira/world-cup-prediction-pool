import type {
  ApiErrorResponse,
  LeaderboardEntry,
  MatchFilters,
  MatchSummary,
  PoolPrediction,
  PoolSummary,
  PredictionResponse,
  RecalculationResponse,
  TokenResponse,
} from "./types";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
export const WORLD_CUP_2026_TOURNAMENT_ID = "11111111-1111-1111-1111-111111111111";

let tokenProvider: (() => string | null) | null = null;
let unauthorizedHandler: (() => void) | null = null;

export function configureApiClient(options: {
  getToken: () => string | null;
  onUnauthorized: () => void;
}) {
  tokenProvider = options.getToken;
  unauthorizedHandler = options.onUnauthorized;
}

export class ApiError extends Error {
  status: number;
  payload?: ApiErrorResponse;

  constructor(status: number, message: string, payload?: ApiErrorResponse) {
    super(message);
    this.status = status;
    this.payload = payload;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");

  const token = tokenProvider?.();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    unauthorizedHandler?.();
  }

  if (!response.ok) {
    const payload = await parseError(response);
    throw new ApiError(response.status, payload?.message ?? response.statusText, payload);
  }

  return response.json() as Promise<T>;
}

async function parseError(response: Response) {
  try {
    return (await response.json()) as ApiErrorResponse;
  } catch {
    return undefined;
  }
}

function queryString(filters: MatchFilters) {
  const params = new URLSearchParams();
  if (filters.group) params.set("group", filters.group.trim());
  if (filters.team) params.set("team", filters.team.trim());
  if (filters.predictableOnly) params.set("predictableOnly", "true");
  const value = params.toString();
  return value ? `?${value}` : "";
}

export const api = {
  register: (body: { username: string; email: string; password: string }) =>
    request<TokenResponse>("/api/v1/auth/register", { method: "POST", body: JSON.stringify(body) }),
  login: (body: { email: string; password: string }) =>
    request<TokenResponse>("/api/v1/auth/token", { method: "POST", body: JSON.stringify(body) }),
  me: () => request<TokenResponse>("/api/v1/auth/me"),
  listPools: () => request<PoolSummary[]>("/api/v1/pools"),
  getPool: (poolId: string) => request<PoolSummary>(`/api/v1/pools/${poolId}`),
  createPool: (body: { name: string; description?: string | null; tournamentId: string }) =>
    request<PoolSummary>("/api/v1/pools", { method: "POST", body: JSON.stringify(body) }),
  joinPool: (inviteCode: string) =>
    request<PoolSummary>("/api/v1/pools/join", { method: "POST", body: JSON.stringify({ inviteCode }) }),
  listMatches: (tournamentId: string, filters: MatchFilters = {}) =>
    request<MatchSummary[]>(`/api/v1/tournaments/${tournamentId}/matches${queryString(filters)}`),
  submitPrediction: (poolId: string, matchId: string, body: { homeScore: number; awayScore: number }) =>
    request<PredictionResponse>(`/api/v1/pools/${poolId}/matches/${matchId}/prediction`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),
  listPredictions: (poolId: string) => request<PoolPrediction[]>(`/api/v1/pools/${poolId}/predictions`),
  leaderboard: (poolId: string) => request<LeaderboardEntry[]>(`/api/v1/pools/${poolId}/leaderboard`),
  upsertResult: (
    matchId: string,
    body: {
      homeScore: number;
      awayScore: number;
      homePenaltyScore?: number | null;
      awayPenaltyScore?: number | null;
      finalResult: boolean;
    },
  ) =>
    request<RecalculationResponse>(`/api/v1/admin/matches/${matchId}/result`, {
      method: "PUT",
      body: JSON.stringify({ ...body, matchId }),
    }),
  resolveParticipants: (matchId: string, body: { homeTeamId: string; awayTeamId: string }) =>
    request<MatchSummary>(`/api/v1/admin/matches/${matchId}/participants`, {
      method: "PUT",
      body: JSON.stringify(body),
    }),
};
