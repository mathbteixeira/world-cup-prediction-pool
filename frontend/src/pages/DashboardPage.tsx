import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { ArrowRight, CalendarClock, Plus, Ticket, Trash2, Trophy } from "lucide-react";
import { api, WORLD_CUP_2026_TOURNAMENT_ID } from "../api/client";
import { useAuth } from "../auth/AuthProvider";
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
  mode: z.enum(["TOURNAMENT", "SINGLE_MATCH"]),
  matchSource: z.enum(["EXISTING", "CUSTOM"]),
  existingTournamentId: z.string().optional(),
  matchId: z.string().optional(),
  homeTeam: z.string().max(100).optional(),
  awayTeam: z.string().max(100).optional(),
  kickoffAt: z.string().optional(),
  competitionLabel: z.string().max(60).optional(),
}).superRefine((values, ctx) => {
  if (values.mode !== "SINGLE_MATCH") return;
  if (values.matchSource === "EXISTING" && !values.matchId) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["matchId"], message: "Choose a match" });
  }
  if (values.matchSource === "EXISTING" && !values.existingTournamentId) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["existingTournamentId"], message: "Choose a tournament" });
  }
  if (values.matchSource === "CUSTOM") {
    if (!values.homeTeam?.trim()) ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["homeTeam"], message: "Home team is required" });
    if (!values.awayTeam?.trim()) ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["awayTeam"], message: "Away team is required" });
    if (!values.kickoffAt) ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["kickoffAt"], message: "Kickoff is required" });
    if (values.homeTeam?.trim().toLowerCase() === values.awayTeam?.trim().toLowerCase()) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["awayTeam"], message: "Teams must be different" });
    }
  }
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
  const { user } = useAuth();
  const poolsQuery = useQuery({ queryKey: ["pools"], queryFn: api.listPools });
  const tournamentsQuery = useQuery({ queryKey: ["tournaments"], queryFn: api.listTournaments });
  const createForm = useForm<CreateForm>({
    resolver: zodResolver(createSchema),
    defaultValues: {
      name: "",
      description: "",
      mode: "TOURNAMENT",
      matchSource: "CUSTOM",
      existingTournamentId: "",
      matchId: "",
      homeTeam: "",
      awayTeam: "",
      kickoffAt: "",
      competitionLabel: "",
    },
  });
  const joinForm = useForm<JoinForm>({ resolver: zodResolver(joinSchema), defaultValues: { inviteCode: "" } });
  const createMode = createForm.watch("mode");
  const matchSource = createForm.watch("matchSource");
  const existingTournamentId = createForm.watch("existingTournamentId");
  const existingMatchesQuery = useQuery({
    queryKey: ["matches", existingTournamentId, "dashboard"],
    queryFn: () => api.listMatches(existingTournamentId!, {}),
    enabled: createMode === "SINGLE_MATCH" && matchSource === "EXISTING" && Boolean(existingTournamentId),
  });

  const createPool = useMutation({
    mutationFn: (values: CreateForm) => {
      if (values.mode === "SINGLE_MATCH" && values.matchSource === "EXISTING") {
        return api.createPool({
          name: values.name,
          description: values.description || null,
          mode: "SINGLE_MATCH",
          matchId: values.matchId!,
        });
      }
      if (values.mode === "SINGLE_MATCH") {
        return api.createPool({
          name: values.name,
          description: values.description || null,
          mode: "SINGLE_MATCH",
          customMatch: {
            homeTeam: values.homeTeam!.trim(),
            awayTeam: values.awayTeam!.trim(),
            kickoffAt: new Date(values.kickoffAt!).toISOString(),
            competitionLabel: values.competitionLabel?.trim() || null,
          },
        });
      }
      return api.createPool({
        name: values.name,
        description: values.description || null,
        mode: "TOURNAMENT",
        tournamentId: WORLD_CUP_2026_TOURNAMENT_ID,
      });
    },
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
  const deletePool = useMutation({
    mutationFn: (poolId: string) => api.deletePool(poolId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["pools"] });
    },
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
                    <div className="flex flex-wrap gap-2">
                      <Badge variant="outline">{pool.poolScope === "SINGLE_MATCH" ? t("singleMatchPool") : t("tournamentPool")}</Badge>
                      <Badge variant={pool.membershipRole === "OWNER" ? "success" : "secondary"}>{pool.membershipRole}</Badge>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="text-sm">
                    <span className="text-muted-foreground">{t("inviteCode")} </span>
                    <span className="font-mono font-semibold">{pool.inviteCode}</span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {(pool.membershipRole === "OWNER" || user?.role === "ADMIN") ? (
                      <Button
                        type="button"
                        variant="outline"
                        disabled={deletePool.isPending}
                        onClick={() => {
                          if (window.confirm(t("deletePoolConfirm"))) {
                            deletePool.mutate(pool.id);
                          }
                        }}
                      >
                        <Trash2 className="h-4 w-4" />
                        {t("deletePool")}
                      </Button>
                    ) : null}
                    <Button asChild>
                      <Link to={`/pools/${pool.id}`}>
                        {t("openPool")}
                        <ArrowRight className="h-4 w-4" />
                      </Link>
                    </Button>
                  </div>
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
                <div className="grid grid-cols-2 gap-2">
                  <Button
                    type="button"
                    variant={createMode === "TOURNAMENT" ? "default" : "outline"}
                    onClick={() => createForm.setValue("mode", "TOURNAMENT")}
                  >
                    <Trophy className="h-4 w-4" />
                    {t("tournamentPool")}
                  </Button>
                  <Button
                    type="button"
                    variant={createMode === "SINGLE_MATCH" ? "default" : "outline"}
                    onClick={() => createForm.setValue("mode", "SINGLE_MATCH")}
                  >
                    <CalendarClock className="h-4 w-4" />
                    {t("singleMatchPool")}
                  </Button>
                </div>
                {createMode === "SINGLE_MATCH" ? (
                  <div className="space-y-4 rounded-md border p-3">
                    <div className="grid grid-cols-2 gap-2">
                      <Button
                        type="button"
                        variant={matchSource === "CUSTOM" ? "default" : "outline"}
                        onClick={() => createForm.setValue("matchSource", "CUSTOM")}
                      >
                        {t("customMatch")}
                      </Button>
                      <Button
                        type="button"
                        variant={matchSource === "EXISTING" ? "default" : "outline"}
                        onClick={() => createForm.setValue("matchSource", "EXISTING")}
                      >
                        {t("existingMatch")}
                      </Button>
                    </div>
                    {matchSource === "EXISTING" ? (
                      <div className="space-y-4">
                        <FormField label={t("tournament")} error={createForm.formState.errors.existingTournamentId}>
                          <select
                            className="h-10 w-full rounded-md border border-input bg-white px-3 text-sm"
                            {...createForm.register("existingTournamentId", {
                              onChange: () => createForm.setValue("matchId", ""),
                            })}
                          >
                            <option value="">{t("selectTournament")}</option>
                            {(tournamentsQuery.data ?? []).map((tournament) => (
                              <option key={tournament.tournamentId} value={tournament.tournamentId}>
                                {tournament.name} {tournament.seasonYear}
                              </option>
                            ))}
                          </select>
                        </FormField>
                        <FormField label={t("match")} error={createForm.formState.errors.matchId}>
                          <select
                            className="h-10 w-full rounded-md border border-input bg-white px-3 text-sm disabled:cursor-not-allowed disabled:bg-muted"
                            disabled={!existingTournamentId}
                            {...createForm.register("matchId")}
                          >
                            <option value="">{existingMatchesQuery.isLoading ? t("loadingMatches") : t("selectMatch")}</option>
                            {(existingMatchesQuery.data ?? []).map((match) => (
                              <option key={match.matchId} value={match.matchId}>
                                {match.homeTeam?.fifaCode ?? match.homePlaceholder ?? "TBD"} vs {match.awayTeam?.fifaCode ?? match.awayPlaceholder ?? "TBD"} -{" "}
                                {new Date(match.kickoffAt).toLocaleString()}
                              </option>
                            ))}
                          </select>
                        </FormField>
                      </div>
                    ) : (
                      <div className="space-y-4">
                        <div className="grid gap-3 sm:grid-cols-2">
                          <FormField label={t("homeTeam")} error={createForm.formState.errors.homeTeam}>
                            <Input {...createForm.register("homeTeam")} />
                          </FormField>
                          <FormField label={t("awayTeam")} error={createForm.formState.errors.awayTeam}>
                            <Input {...createForm.register("awayTeam")} />
                          </FormField>
                        </div>
                        <FormField label={t("kickoff")} error={createForm.formState.errors.kickoffAt}>
                          <Input type="datetime-local" {...createForm.register("kickoffAt")} />
                        </FormField>
                        <FormField label={t("competitionLabel")} error={createForm.formState.errors.competitionLabel}>
                          <Input {...createForm.register("competitionLabel")} />
                        </FormField>
                      </div>
                    )}
                  </div>
                ) : null}
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
