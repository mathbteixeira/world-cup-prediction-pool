/**
 * @vitest-environment jsdom
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LanguageProvider } from "../i18n/LanguageProvider";
import { AdminPage } from "./AdminPage";

const apiMock = vi.hoisted(() => ({
  listMatches: vi.fn(),
  listPlayers: vi.fn(),
  resolveParticipants: vi.fn(),
  upsertResult: vi.fn(),
  confirmTopScorer: vi.fn(),
  listAdminPools: vi.fn(),
  deleteAdminPool: vi.fn(),
  listPoolMembers: vi.fn(),
  addPoolMember: vi.fn(),
  removePoolMember: vi.fn(),
  transferPoolOwnership: vi.fn(),
}));

const authState = vi.hoisted(() => ({
  user: { userId: "admin-1", username: "admin", email: "admin@example.com", role: "ADMIN" },
}));

vi.mock("../api/client", () => ({
  WORLD_CUP_2026_TOURNAMENT_ID: "11111111-1111-1111-1111-111111111111",
  api: apiMock,
}));

vi.mock("../auth/AuthProvider", () => ({
  useAuth: () => ({ user: authState.user }),
}));

describe("AdminPage", () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.clearAllMocks();
    authState.user = { userId: "admin-1", username: "admin", email: "admin@example.com", role: "ADMIN" };
    apiMock.listMatches.mockResolvedValue([
      {
        matchId: "match-1",
        groupName: "A",
        kickoffAt: "2026-06-30T12:00:00Z",
        stage: "GROUP_STAGE",
        status: "SCHEDULED",
        predictionOpen: true,
        homeTeam: { id: "team-1", name: "Brazil", fifaCode: "BRA" },
        awayTeam: { id: "team-2", name: "Spain", fifaCode: "ESP" },
        homePlaceholder: null,
        awayPlaceholder: null,
        result: null,
      },
      {
        matchId: "match-2",
        groupName: null,
        kickoffAt: "2026-07-10T12:00:00Z",
        stage: "QUARTER_FINAL",
        status: "SCHEDULED",
        predictionOpen: false,
        homeTeam: null,
        awayTeam: null,
        homePlaceholder: "W89",
        awayPlaceholder: "W90",
        result: null,
      },
    ]);
    apiMock.resolveParticipants.mockResolvedValue({});
    apiMock.upsertResult.mockResolvedValue({ scoredPredictions: 0, affectedPools: 0 });
    apiMock.listPlayers.mockResolvedValue([
      { id: "player-1", teamId: "team-1", name: "Brazil Player 01", rosterNumber: 1 },
      { id: "player-2", teamId: "team-1", name: "Brazil Player 02", rosterNumber: 2 },
    ]);
    apiMock.confirmTopScorer.mockResolvedValue({ scoredPredictions: 1, affectedPools: 1 });
    apiMock.listAdminPools.mockResolvedValue([
      {
        id: "pool-1",
        tournamentId: "11111111-1111-1111-1111-111111111111",
        singleMatchId: null,
        poolScope: "TOURNAMENT",
        name: "Family Pool",
        description: "",
        inviteCode: "INVITE01",
        owner: { userId: "owner-1", username: "owner", email: "owner@example.com", role: "OWNER" },
        memberCount: 2,
      },
    ]);
    apiMock.listPoolMembers.mockResolvedValue([
      { userId: "owner-1", username: "owner", email: "owner@example.com", role: "OWNER" },
      { userId: "member-1", username: "member", email: "member@example.com", role: "MEMBER" },
    ]);
    apiMock.addPoolMember.mockResolvedValue({});
    apiMock.transferPoolOwnership.mockResolvedValue({});
  });

  it("allows admins to update knockout teams from the admin page", async () => {
    renderAdminPage();

    await screen.findByText("Atualização de times do mata-mata");
    await screen.findByText(/W89/);
    await userEvent.selectOptions(screen.getByLabelText("Jogo de mata-mata"), "match-2");
    await userEvent.selectOptions(screen.getAllByLabelText("Mandante")[0], "team-1");
    await userEvent.selectOptions(screen.getAllByLabelText("Visitante")[0], "team-2");
    await userEvent.click(screen.getByRole("button", { name: "Atualizar times" }));

    await waitFor(() =>
      expect(apiMock.resolveParticipants).toHaveBeenCalledWith("match-2", {
        homeTeamId: "team-1",
        awayTeamId: "team-2",
      }),
    );
  });

  it("allows admins to add a pool member and transfer ownership", async () => {
    renderAdminPage();

    await screen.findByText("Family Pool");
    await userEvent.type(screen.getByLabelText("Email do membro"), "new@example.com");
    await userEvent.click(screen.getByRole("button", { name: "Adicionar membro" }));
    await userEvent.type(screen.getByLabelText("Email do novo dono"), "member@example.com");
    await userEvent.click(screen.getByRole("button", { name: "Transferir dono" }));

    await waitFor(() => expect(apiMock.addPoolMember).toHaveBeenCalledWith("pool-1", { email: "new@example.com" }));
    await waitFor(() => expect(apiMock.transferPoolOwnership).toHaveBeenCalledWith("pool-1", { email: "member@example.com" }));
  });

  it("allows admins to confirm the tournament top scorer", async () => {
    renderAdminPage();

    await screen.findByText("Atualização de goleador");
    await waitFor(() => expect((screen.getByLabelText("País do goleador") as HTMLSelectElement).options.length).toBeGreaterThan(1));
    await userEvent.selectOptions(screen.getByLabelText("País do goleador"), "team-1");
    await waitFor(() =>
      expect(apiMock.listPlayers).toHaveBeenCalledWith("11111111-1111-1111-1111-111111111111", "team-1"),
    );
    await userEvent.selectOptions(screen.getByLabelText("Jogador goleador"), "player-1");
    await userEvent.selectOptions(screen.getByLabelText("Gols previstos"), "7");
    await userEvent.click(screen.getByRole("button", { name: "Confirmar goleador" }));

    await waitFor(() =>
      expect(apiMock.confirmTopScorer).toHaveBeenCalledWith("11111111-1111-1111-1111-111111111111", {
        playerId: "player-1",
        goals: 7,
      }),
    );
  });

  it("hides admin actions from non-admin users", () => {
    authState.user = { userId: "user-1", username: "alice", email: "alice@example.com", role: "USER" };

    renderAdminPage();

    expect(screen.getByText("Acesso de admin necessário")).toBeInTheDocument();
    expect(apiMock.listAdminPools).not.toHaveBeenCalled();
  });
});

function renderAdminPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <LanguageProvider>
        <AdminPage />
      </LanguageProvider>
    </QueryClientProvider>,
  );
}
