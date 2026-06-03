import { Link, Navigate } from "react-router-dom";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { UserPlus } from "lucide-react";
import { AuthShell } from "./AuthShell";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Alert, AlertDescription } from "../components/ui/alert";
import { FormField } from "../components/FormField";
import { useAuth } from "../auth/AuthProvider";
import { useLanguage } from "../i18n/LanguageProvider";

const schema = z.object({
  username: z.string().min(3).max(50),
  email: z.string().email(),
  password: z.string().min(8, "Use at least 8 characters"),
});

type RegisterForm = z.infer<typeof schema>;

export function RegisterPage() {
  const { register: registerUser, token } = useAuth();
  const { t } = useLanguage();
  const form = useForm<RegisterForm>({
    resolver: zodResolver(schema),
    defaultValues: { username: "", email: "", password: "" },
  });

  if (token) return <Navigate to="/" replace />;

  async function onSubmit(values: RegisterForm) {
    try {
      await registerUser(values.username, values.email, values.password);
    } catch (error) {
      form.setError("root", { message: error instanceof Error ? error.message : "Registration failed" });
    }
  }

  return (
    <AuthShell title={t("createAccount")} subtitle={t("registerSubtitle")}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        {form.formState.errors.root ? (
          <Alert className="border-destructive/30">
            <AlertDescription>{form.formState.errors.root.message}</AlertDescription>
          </Alert>
        ) : null}
        <FormField label={t("username")} error={form.formState.errors.username}>
          <Input autoComplete="username" {...form.register("username")} />
        </FormField>
        <FormField label={t("email")} error={form.formState.errors.email}>
          <Input type="email" autoComplete="email" {...form.register("email")} />
        </FormField>
        <FormField label={t("password")} error={form.formState.errors.password}>
          <Input type="password" autoComplete="new-password" {...form.register("password")} />
        </FormField>
        <Button className="w-full" disabled={form.formState.isSubmitting}>
          <UserPlus className="h-4 w-4" />
          {t("register")}
        </Button>
        <p className="text-center text-sm text-muted-foreground">
          {t("alreadyRegistered")}{" "}
          <Link className="font-medium text-primary underline-offset-4 hover:underline" to="/login">
            {t("signIn")}
          </Link>
        </p>
      </form>
    </AuthShell>
  );
}
