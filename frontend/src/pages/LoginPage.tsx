import { Link, Navigate, useLocation } from "react-router-dom";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { LogIn } from "lucide-react";
import { AuthShell } from "./AuthShell";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Alert, AlertDescription } from "../components/ui/alert";
import { FormField } from "../components/FormField";
import { useAuth } from "../auth/AuthProvider";

const schema = z.object({
  email: z.string().email(),
  password: z.string().min(1, "Password is required"),
});

type LoginForm = z.infer<typeof schema>;

export function LoginPage() {
  const { login, token } = useAuth();
  const location = useLocation();
  const form = useForm<LoginForm>({ resolver: zodResolver(schema), defaultValues: { email: "", password: "" } });
  const from = (location.state as { from?: Location } | null)?.from?.pathname ?? "/";

  if (token) return <Navigate to={from} replace />;

  async function onSubmit(values: LoginForm) {
    try {
      await login(values.email, values.password);
    } catch (error) {
      form.setError("root", { message: error instanceof Error ? error.message : "Login failed" });
    }
  }

  return (
    <AuthShell title="Sign in" subtitle="Open your pool dashboard">
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        {form.formState.errors.root ? (
          <Alert className="border-destructive/30">
            <AlertDescription>{form.formState.errors.root.message}</AlertDescription>
          </Alert>
        ) : null}
        <FormField label="Email" error={form.formState.errors.email}>
          <Input type="email" autoComplete="email" {...form.register("email")} />
        </FormField>
        <FormField label="Password" error={form.formState.errors.password}>
          <Input type="password" autoComplete="current-password" {...form.register("password")} />
        </FormField>
        <Button className="w-full" disabled={form.formState.isSubmitting}>
          <LogIn className="h-4 w-4" />
          Sign in
        </Button>
        <p className="text-center text-sm text-muted-foreground">
          New here?{" "}
          <Link className="font-medium text-primary underline-offset-4 hover:underline" to="/register">
            Create an account
          </Link>
        </p>
      </form>
    </AuthShell>
  );
}
