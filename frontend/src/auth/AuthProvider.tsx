import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api, configureApiClient } from "../api/client";
import type { TokenResponse } from "../api/types";
import { clearStoredToken, getStoredToken, storeToken } from "./tokenStorage";

type AuthContextValue = {
  token: string | null;
  user: TokenResponse | null;
  bootstrapping: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const queryClient = useQueryClient();
  const [token, setToken] = useState<string | null>(() => getStoredToken());
  const [user, setUser] = useState<TokenResponse | null>(null);
  const [bootstrapping, setBootstrapping] = useState(Boolean(getStoredToken()));

  const logout = useCallback(() => {
    clearStoredToken();
    setToken(null);
    setUser(null);
    queryClient.clear();
  }, [queryClient]);

  useEffect(() => {
    configureApiClient({
      getToken: () => getStoredToken(),
      onUnauthorized: logout,
    });
  }, [logout]);

  useEffect(() => {
    let active = true;
    if (!token) {
      setBootstrapping(false);
      return;
    }
    setBootstrapping(true);
    api
      .me()
      .then((me) => {
        if (active) setUser(me);
      })
      .catch(() => {
        if (active) logout();
      })
      .finally(() => {
        if (active) setBootstrapping(false);
      });
    return () => {
      active = false;
    };
  }, [logout, token]);

  const applySession = useCallback((response: TokenResponse) => {
    storeToken(response.accessToken);
    setToken(response.accessToken);
    setUser(response);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      user,
      bootstrapping,
      login: async (email, password) => applySession(await api.login({ email, password })),
      register: async (username, email, password) => applySession(await api.register({ username, email, password })),
      logout,
    }),
    [applySession, bootstrapping, logout, token, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return value;
}
