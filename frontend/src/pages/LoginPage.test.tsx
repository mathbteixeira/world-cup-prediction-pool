/**
 * @vitest-environment jsdom
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../api/client";
import { LanguageProvider } from "../i18n/LanguageProvider";
import { LoginPage } from "./LoginPage";

const authMock = vi.hoisted(() => ({
  login: vi.fn(),
}));

vi.mock("../auth/AuthProvider", () => ({
  useAuth: () => ({
    token: null,
    user: null,
    bootstrapping: false,
    login: authMock.login,
    register: vi.fn(),
    logout: vi.fn(),
  }),
}));

describe("LoginPage", () => {
  beforeEach(() => {
    window.localStorage.clear();
    authMock.login.mockReset();
  });

  it("shows a warning message for invalid credentials", async () => {
    authMock.login.mockRejectedValue(new ApiError(401, "Unauthorized"));

    renderLoginPage();

    await userEvent.type(screen.getByLabelText(/email/i), "missing@example.com");
    await userEvent.type(screen.getByLabelText(/password/i), "wrong-password");
    await userEvent.click(screen.getByRole("button", { name: /entrar/i }));

    expect(await screen.findByText("Não foi possível entrar")).toBeInTheDocument();
    expect(screen.getByText("Não encontramos uma conta com este email, ou a senha está incorreta.")).toBeInTheDocument();
    expect(document.querySelector("svg")).toBeInTheDocument();
  });
});

function renderLoginPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <LanguageProvider>
        <MemoryRouter>
          <LoginPage />
        </MemoryRouter>
      </LanguageProvider>
    </QueryClientProvider>,
  );
}
