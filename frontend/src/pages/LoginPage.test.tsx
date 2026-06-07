/**
 * @vitest-environment jsdom
 */
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactElement } from "react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../api/client";
import { LanguageProvider } from "../i18n/LanguageProvider";
import { LoginPage } from "./LoginPage";
import { RegisterPage } from "./RegisterPage";

const authMock = vi.hoisted(() => ({
  login: vi.fn(),
  register: vi.fn(),
}));

vi.mock("../auth/AuthProvider", () => ({
  useAuth: () => ({
    token: null,
    user: null,
    bootstrapping: false,
    login: authMock.login,
    register: authMock.register,
    logout: vi.fn(),
  }),
}));

describe("LoginPage", () => {
  beforeEach(() => {
    window.localStorage.clear();
    authMock.login.mockReset();
    authMock.register.mockReset();
  });

  it("shows a warning message for invalid credentials", async () => {
    authMock.login.mockRejectedValue(new ApiError(401, "Unauthorized"));

    renderLoginPage();

    await userEvent.type(screen.getByLabelText("Email"), "missing@example.com");
    await userEvent.type(screen.getByLabelText("Senha"), "wrong-password");
    await userEvent.click(screen.getByRole("button", { name: /entrar/i }));

    expect(await screen.findByText("Não foi possível entrar")).toBeInTheDocument();
    expect(screen.getByText("Não encontramos uma conta com este email, ou a senha está incorreta.")).toBeInTheDocument();
  });

  it("submits registration details through the auth flow", async () => {
    authMock.register.mockResolvedValue(undefined);

    renderAuthPage(<RegisterPage />);

    await userEvent.type(screen.getByLabelText("Usuário"), "maria");
    await userEvent.type(screen.getByLabelText("Email"), "maria@example.com");
    await userEvent.type(screen.getByLabelText("Senha"), "password123");
    await userEvent.click(screen.getByRole("button", { name: /cadastrar/i }));

    expect(authMock.register).toHaveBeenCalledWith("maria", "maria@example.com", "password123");
  });
});

function renderLoginPage() {
  return renderAuthPage(<LoginPage />);
}

function renderAuthPage(page: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <LanguageProvider>
        <MemoryRouter>
          {page}
        </MemoryRouter>
      </LanguageProvider>
    </QueryClientProvider>,
  );
}
