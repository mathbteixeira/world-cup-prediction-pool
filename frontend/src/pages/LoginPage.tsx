import { Link, Navigate, useLocation } from "react-router-dom";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { LogIn, TriangleAlert } from "lucide-react";
import { AuthShell } from "./AuthShell";
import { ApiError } from "../api/client";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Alert, AlertDescription, AlertTitle } from "../components/ui/alert";
import { FormField } from "../components/FormField";
import { useAuth } from "../auth/AuthProvider";
import { useLanguage } from "../i18n/LanguageProvider";

const schema = z.object({
  email: z.string().email(),
  password: z.string().min(1, "Password is required"),
});

type LoginForm = z.infer<typeof schema>;

export function LoginPage() {
  const { login, token } = useAuth();
  const { t } = useLanguage();
  const location = useLocation();
  const form = useForm<LoginForm>({ resolver: zodResolver(schema), defaultValues: { email: "", password: "" } });
  const from = (location.state as { from?: Location } | null)?.from?.pathname ?? "/";

  if (token) return <Navigate to={from} replace />;

  async function onSubmit(values: LoginForm) {
    try {
      await login(values.email, values.password);
    } catch (error) {
      form.setError("root", { message: loginErrorMessage(error, t) });
    }
  }

  return (
    <AuthShell title={t("signIn")} subtitle={t("signInSubtitle")}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        {form.formState.errors.root ? (
          <Alert className="border-destructive/40 bg-destructive/5 text-destructive">
            <div className="flex items-start gap-3">
              <TriangleAlert className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
              <div>
                <AlertTitle>{t("loginErrorTitle")}</AlertTitle>
                <AlertDescription className="text-destructive/90">{form.formState.errors.root.message}</AlertDescription>
              </div>
            </div>
          </Alert>
        ) : null}
        <FormField label={t("email")} error={form.formState.errors.email}>
          <Input type="email" autoComplete="email" {...form.register("email")} />
        </FormField>
        <FormField label={t("password")} error={form.formState.errors.password}>
          <Input type="password" autoComplete="current-password" {...form.register("password")} />
        </FormField>
        <Button className="w-full" disabled={form.formState.isSubmitting}>
          <LogIn className="h-4 w-4" />
          {t("signIn")}
        </Button>
        <p className="text-center text-sm text-muted-foreground">
          {t("newHere")}{" "}
          <Link className="font-medium text-primary underline-offset-4 hover:underline" to="/register">
            {t("createAccount")}
          </Link>
        </p>
      </form>
    </AuthShell>
  );
}

function loginErrorMessage(error: unknown, t: (key: "loginFailed" | "loginInvalidCredentials") => string) {
  if (error instanceof ApiError && error.status === 401) {
    return t("loginInvalidCredentials");
  }
  return error instanceof Error && error.message ? error.message : t("loginFailed");
}
