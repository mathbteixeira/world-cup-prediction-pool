/**
 * @vitest-environment jsdom
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LanguageProvider } from "../i18n/LanguageProvider";
import { DashboardPage } from "./DashboardPage";

const apiMock = vi.hoisted(() => ({
  listPools: vi.fn(),
  listTournaments: vi.fn(),
  listMatches: vi.fn(),
  createPool: vi.fn(),
  joinPool: vi.fn(),
  deletePool: vi.fn(),
}));

vi.mock("../api/client", () => ({
  WORLD_CUP_2026_TOURNAMENT_ID: "11111111-1111-1111-1111-111111111111",
  api: apiMock,
}));

vi.mock("../auth/AuthProvider", () => ({
  useAuth: () => ({
    user: { username: "alice", email: "alice@example.com", role: "USER" },
  }),
}));

describe("DashboardPage", () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.clearAllMocks();
    apiMock.listPools.mockResolvedValue([]);
    apiMock.listTournaments.mockResolvedValue([]);
    apiMock.listMatches.mockResolvedValue([]);
    apiMock.createPool.mockResolvedValue({ id: "created-pool" });
    apiMock.joinPool.mockResolvedValue({ id: "joined-pool" });
  });

  it("creates a tournament pool", async () => {
    renderDashboardPage();

    await userEvent.type(inputByName("name"), "Family Cup");
    await userEvent.click(screen.getByRole("button", { name: "Criar" }));

    await waitFor(() => expect(apiMock.createPool).toHaveBeenCalled());
    expect(apiMock.createPool).toHaveBeenCalledWith({
      name: "Family Cup",
      description: null,
      mode: "TOURNAMENT",
      tournamentId: "11111111-1111-1111-1111-111111111111",
    });
  });

  it("joins a pool with an invite code", async () => {
    renderDashboardPage();

    await userEvent.type(inputByName("inviteCode"), "abc123");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));

    await waitFor(() => expect(apiMock.joinPool).toHaveBeenCalledWith("ABC123"));
  });
});

function renderDashboardPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <LanguageProvider>
        <MemoryRouter initialEntries={["/"]}>
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/pools/:poolId" element={<p>Pool detail</p>} />
          </Routes>
        </MemoryRouter>
      </LanguageProvider>
    </QueryClientProvider>,
  );
}

function inputByName(name: string) {
  const input = document.querySelector<HTMLInputElement>(`input[name="${name}"]`);
  if (!input) throw new Error(`Missing input ${name}`);
  return input;
}
