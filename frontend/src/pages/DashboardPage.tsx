import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { ArrowRight, Plus, Ticket, Trophy } from "lucide-react";
import { api, WORLD_CUP_2026_TOURNAMENT_ID } from "../api/client";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { Textarea } from "../components/ui/textarea";
import { Badge } from "../components/ui/badge";
import { Alert, AlertDescription, AlertTitle } from "../components/ui/alert";
import { FormField } from "../components/FormField";
import { useLanguage } from "../i18n/LanguageProvider";

const createSchema = z.object({
  name: z.string().min(3, "Use at least 3 characters").max(120),
  description: z.string().max(500).optional(),
});

const joinSchema = z.object({
  inviteCode: z.string().min(6).max(20),
});

type CreateForm = z.infer<typeof createSchema>;
type JoinForm = z.infer<typeof joinSchema>;

export function DashboardPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { t } = useLanguage();
  const poolsQuery = useQuery({ queryKey: ["pools"], queryFn: api.listPools });
  const createForm = useForm<CreateForm>({ resolver: zodResolver(createSchema), defaultValues: { name: "", description: "" } });
  const joinForm = useForm<JoinForm>({ resolver: zodResolver(joinSchema), defaultValues: { inviteCode: "" } });

  const createPool = useMutation({
    mutationFn: (values: CreateForm) =>
      api.createPool({
        name: values.name,
        description: values.description || null,
        tournamentId: WORLD_CUP_2026_TOURNAMENT_ID,
      }),
    onSuccess: async (pool) => {
      await queryClient.invalidateQueries({ queryKey: ["pools"] });
      navigate(`/pools/${pool.id}`);
    },
    onError: (error) => createForm.setError("root", { message: error instanceof Error ? error.message : "Create failed" }),
  });

  const joinPool = useMutation({
    mutationFn: (values: JoinForm) => api.joinPool(values.inviteCode.toUpperCase()),
    onSuccess: async (pool) => {
      await queryClient.invalidateQueries({ queryKey: ["pools"] });
      navigate(`/pools/${pool.id}`);
    },
    onError: (error) => joinForm.setError("root", { message: error instanceof Error ? error.message : "Join failed" }),
  });

  const pools = poolsQuery.data ?? [];

  return (
    <div className="space-y-6">
      <section className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-normal">{t("poolDashboardTitle")}</h1>
          <p className="text-sm text-muted-foreground">{t("poolDashboardSubtitle")}</p>
        </div>
        <Badge variant="outline">{t("worldCup2026")}</Badge>
      </section>

      <section className="grid gap-4 lg:grid-cols-[1fr_380px]">
        <div className="space-y-4">
          {poolsQuery.isLoading ? (
            <Card>
              <CardContent className="pt-5 text-sm text-muted-foreground">{t("loadingPools")}</CardContent>
            </Card>
          ) : pools.length === 0 ? (
            <Card>
              <CardHeader>
                <CardTitle>{t("noPools")}</CardTitle>
                <CardDescription>{t("noPoolsDesc")}</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Trophy className="h-4 w-4" />
                  {t("seededTournamentId")}: {WORLD_CUP_2026_TOURNAMENT_ID}
                </div>
              </CardContent>
            </Card>
          ) : (
            pools.map((pool) => (
              <Card key={pool.id}>
                <CardHeader className="pb-3">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <CardTitle>{pool.name}</CardTitle>
                      <CardDescription>{pool.description || t("noDescription")}</CardDescription>
                    </div>
                    <Badge variant={pool.membershipRole === "OWNER" ? "success" : "secondary"}>{pool.membershipRole}</Badge>
                  </div>
                </CardHeader>
                <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="text-sm">
                    <span className="text-muted-foreground">{t("inviteCode")} </span>
                    <span className="font-mono font-semibold">{pool.inviteCode}</span>
                  </div>
                  <Button asChild>
                    <Link to={`/pools/${pool.id}`}>
                      {t("openPool")}
                      <ArrowRight className="h-4 w-4" />
                    </Link>
                  </Button>
                </CardContent>
              </Card>
            ))
          )}
        </div>

        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>{t("createPool")}</CardTitle>
              <CardDescription>{t("createPoolDesc")}</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={createForm.handleSubmit((values) => createPool.mutate(values))} className="space-y-4">
                {createForm.formState.errors.root ? (
                  <Alert className="border-destructive/30">
                    <AlertDescription>{createForm.formState.errors.root.message}</AlertDescription>
                  </Alert>
                ) : null}
                <FormField label={t("poolName")} error={createForm.formState.errors.name}>
                  <Input {...createForm.register("name")} />
                </FormField>
                <FormField label={t("description")} error={createForm.formState.errors.description}>
                  <Textarea {...createForm.register("description")} />
                </FormField>
                <Button className="w-full" disabled={createPool.isPending}>
                  <Plus className="h-4 w-4" />
                  {t("create")}
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>{t("joinPool")}</CardTitle>
              <CardDescription>{t("joinPoolDesc")}</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={joinForm.handleSubmit((values) => joinPool.mutate(values))} className="space-y-4">
                {joinForm.formState.errors.root ? (
                  <Alert className="border-destructive/30">
                    <AlertDescription>{joinForm.formState.errors.root.message}</AlertDescription>
                  </Alert>
                ) : null}
                <FormField label={t("inviteCode")} error={joinForm.formState.errors.inviteCode}>
                  <Input className="font-mono uppercase" {...joinForm.register("inviteCode")} />
                </FormField>
                <Button className="w-full" variant="secondary" disabled={joinPool.isPending}>
                  <Ticket className="h-4 w-4" />
                  {t("join")}
                </Button>
              </form>
            </CardContent>
          </Card>

          <Alert>
            <AlertTitle>{t("scoringModel")}</AlertTitle>
            <AlertDescription>
              {t("scoringModelDesc")}
            </AlertDescription>
          </Alert>
        </div>
      </section>
    </div>
  );
}
