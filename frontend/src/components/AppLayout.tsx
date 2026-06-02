import { Link, NavLink, Outlet } from "react-router-dom";
import { LogOut, Shield, Trophy } from "lucide-react";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { useAuth } from "../auth/AuthProvider";
import { cn } from "../lib/utils";

export function AppLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b bg-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-3 px-4 py-4 sm:flex-row sm:items-center sm:justify-between lg:px-6">
          <Link to="/" className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-md bg-primary text-primary-foreground">
              <Trophy className="h-5 w-5" />
            </div>
            <div>
              <p className="text-base font-semibold leading-tight">World Cup Pool Ops</p>
              <p className="text-xs text-muted-foreground">Prediction, scoring, and leaderboard control</p>
            </div>
          </Link>
          <div className="flex flex-wrap items-center gap-2">
            <NavLink
              to="/"
              className={({ isActive }) =>
                cn("rounded-md px-3 py-2 text-sm font-medium hover:bg-muted", isActive && "bg-muted")
              }
            >
              Dashboard
            </NavLink>
            {user?.role === "ADMIN" ? (
              <Badge variant="warning" className="gap-1">
                <Shield className="h-3 w-3" />
                ADMIN
              </Badge>
            ) : null}
            <span className="text-sm text-muted-foreground">{user?.username}</span>
            <Button variant="outline" size="sm" onClick={logout}>
              <LogOut className="h-4 w-4" />
              Logout
            </Button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6 lg:px-6">
        <Outlet />
      </main>
    </div>
  );
}
