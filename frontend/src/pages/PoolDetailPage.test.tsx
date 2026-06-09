/**
 * @vitest-environment jsdom
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LanguageProvider } from "../i18n/LanguageProvider";
import { PoolDetailPage } from "./PoolDetailPage";

const apiMock = vi.hoisted(() => ({
  getPool: vi.fn(),
  listMatches: vi.fn(),
  listPredictions: vi.fn(),
  leaderboard: vi.fn(),
  listManagedParticipants: vi.fn(),
  submitPrediction: vi.fn(),
}));

vi.mock("../api/client", () => ({
  ApiError: class ApiError extends Error {
    status: number;

    constructor(status: number, message: string) {
      super(message);
      this.status = status;
    }
  },
  WORLD_CUP_2026_TOURNAMENT_ID: "11111111-1111-1111-1111-111111111111",
  api: apiMock,
}));

vi.mock("../auth/AuthProvider", () => ({
  useAuth: () => ({
    user: { userId: "user-1", username: "alice", email: "alice@example.com", role: "USER", accessToken: "token" },
  }),
}));

describe("PoolDetailPage", () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.clearAllMocks();
    apiMock.getPool.mockResolvedValue({
      poolId: "pool-1",
      name: "Family Pool",
      description: "",
      inviteCode: "INVITE01",
      tournamentId: "11111111-1111-1111-1111-111111111111",
      poolScope: "TOURNAMENT",
      singleMatchId: null,
      membershipRole: "MEMBER",
    });
    apiMock.listMatches.mockResolvedValue([
      {
        matchId: "match-1",
        groupName: "A",
        kickoffAt: "2026-06-01T12:00:00Z",
        status: "SCHEDULED",
        predictionOpen: false,
        homeTeam: { id: "team-1", name: "Brazil", fifaCode: "BRA" },
        awayTeam: { id: "team-2", name: "Spain", fifaCode: "ESP" },
        homePlaceholder: null,
        awayPlaceholder: null,
        result: null,
      },
    ]);
    apiMock.listPredictions.mockResolvedValue([]);
    apiMock.leaderboard.mockResolvedValue([]);
    apiMock.listManagedParticipants.mockResolvedValue([]);
    apiMock.submitPrediction.mockResolvedValue({
      predictionId: "prediction-1",
      poolId: "pool-1",
      matchId: "match-1",
      homeScore: 2,
      awayScore: 1,
      submittedAt: "2026-05-31T12:00:00Z",
    });
  });

  it("shows a Portuguese warning when saving after kickoff", async () => {
    renderPoolDetailPage();

    await waitFor(() => expect(screen.getByText("Family Pool")).toBeInTheDocument());
    await userEvent.click(await screen.findByRole("button", { name: "Salvar" }));

    expect(screen.getByText("Os palpites estão fechados para este jogo porque o horário de início já passou.")).toBeInTheDocument();
    expect(apiMock.submitPrediction).not.toHaveBeenCalled();
  });

  it("submits a prediction for an open match", async () => {
    apiMock.listMatches.mockResolvedValue([
      {
        matchId: "match-1",
        groupName: "A",
        kickoffAt: "2026-06-30T12:00:00Z",
        status: "SCHEDULED",
        predictionOpen: true,
        homeTeam: { id: "team-1", name: "Brazil", fifaCode: "BRA" },
        awayTeam: { id: "team-2", name: "Spain", fifaCode: "ESP" },
        homePlaceholder: null,
        awayPlaceholder: null,
        result: null,
      },
    ]);

    renderPoolDetailPage();

    await waitFor(() => expect(screen.getByText("Family Pool")).toBeInTheDocument());
    await userEvent.type(await screen.findByLabelText("BRA prediction"), "2");
    await userEvent.type(await screen.findByLabelText("ESP prediction"), "1");
    await userEvent.click(await screen.findByRole("button", { name: "Salvar" }));

    await waitFor(() =>
      expect(apiMock.submitPrediction).toHaveBeenCalledWith("pool-1", "match-1", {
        homeScore: 2,
        awayScore: 1,
      }),
    );
  });
});

function renderPoolDetailPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <LanguageProvider>
        <MemoryRouter initialEntries={["/pools/pool-1"]}>
          <Routes>
            <Route path="/pools/:poolId" element={<PoolDetailPage />} />
          </Routes>
        </MemoryRouter>
      </LanguageProvider>
    </QueryClientProvider>,
  );
}
