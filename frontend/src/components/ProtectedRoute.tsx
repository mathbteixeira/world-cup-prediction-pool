import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";

export function ProtectedRoute() {
  const { token, bootstrapping } = useAuth();
  const location = useLocation();

  if (bootstrapping) {
    return <div className="p-6 text-sm text-muted-foreground">Restoring session...</div>;
  }

  if (!token) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
