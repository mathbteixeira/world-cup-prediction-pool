import { useMemo, useState, type SelectHTMLAttributes } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient, type UseMutationResult } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, type UseFormReturn } from "react-hook-form";
import { z } from "zod";
import { ArrowLeft, CheckCircle2, Clipboard, Filter, RefreshCw, Shield } from "lucide-react";
import { api } from "../api/client";
import type { MatchSummary, PoolPrediction, RecalculationResponse, TeamSummary } from "../api/types";
import { useAuth } from "../auth/AuthProvider";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { Alert, AlertDescription, AlertTitle } from "../components/ui/alert";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../components/ui/tabs";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../components/ui/table";
import { FormField } from "../components/FormField";
import { useLanguage } from "../i18n/LanguageProvider";
import { flagForFifaCode } from "../lib/flags";
import { formatDateTime } from "../lib/utils";

type ScoreDraft = Record<string, { homeScore: string; awayScore: string }>;

const resultSchema = z.object({
  matchId: z.string().min(1, "Choose a match"),
  homeScore: z.coerce.number().int().min(0),
  awayScore: z.coerce.number().int().min(0),
  homePenaltyScore: z.union([z.literal(""), z.coerce.number().int().min(0)]).optional(),
  awayPenaltyScore: z.union([z.literal(""), z.coerce.number().int().min(0)]).optional(),
  finalResult: z.boolean(),
});

type ResultForm = z.infer<typeof resultSchema>;

const participantSchema = z.object({
  matchId: z.string().min(1, "Choose a placeholder match"),
  homeTeamId: z.string().min(1, "Choose a home team"),
  awayTeamId: z.string().min(1, "Choose an away team"),
});

type ParticipantForm = z.infer<typeof participantSchema>;

export function PoolDetailPage() {
  const { poolId = "" } = useParams();
  const { user } = useAuth();
  const { t } = useLanguage();
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState({ group: "", team: "", predictableOnly: false });
  const [drafts, setDrafts] = useState<ScoreDraft>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [recalculation, setRecalculation] = useState<RecalculationResponse | null>(null);

  const poolQuery = useQuery({ queryKey: ["pool", poolId], queryFn: () => api.getPool(poolId), enabled: Boolean(poolId) });
  const tournamentId = poolQuery.data?.tournamentId;
  const matchesQuery = useQuery({
    queryKey: ["matches", tournamentId, filters],
    queryFn: () =>
      api.listMatches(tournamentId!, {
        group: filters.group,
        team: filters.team,
        predictableOnly: filters.predictableOnly,
      }),
    enabled: Boolean(tournamentId),
  });
  const allMatchesQuery = useQuery({
    queryKey: ["matches", tournamentId, "all"],
    queryFn: () => api.listMatches(tournamentId!, {}),
    enabled: Boolean(tournamentId),
  });
  const predictionsQuery = useQuery({
    queryKey: ["predictions", poolId],
    queryFn: () => api.listPredictions(poolId),
    enabled: Boolean(poolId),
  });
  const leaderboardQuery = useQuery({
    queryKey: ["leaderboard", poolId],
    queryFn: () => api.leaderboard(poolId),
    enabled: Boolean(poolId),
  });

  const myPredictions = useMemo(() => {
    const map = new Map<string, PoolPrediction>();
    for (const prediction of predictionsQuery.data ?? []) {
      if (prediction.mine) map.set(prediction.match.matchId, prediction);
    }
    return map;
  }, [predictionsQuery.data]);

  const submitPrediction = useMutation({
    mutationFn: ({ matchId, homeScore, awayScore }: { matchId: string; homeScore: number; awayScore: number }) =>
      api.submitPrediction(poolId, matchId, { homeScore, awayScore }),
    onSuccess: async () => {
      setSuccessMessage(t("predictionSaved"));
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["predictions", poolId] }),
        queryClient.invalidateQueries({ queryKey: ["leaderboard", poolId] }),
      ]);
    },
    onError: (error) => setSuccessMessage(error instanceof Error ? error.message : "Prediction failed"),
  });

  const resultForm = useForm<ResultForm>({
    resolver: zodResolver(resultSchema),
    defaultValues: { matchId: "", homeScore: 0, awayScore: 0, homePenaltyScore: "", awayPenaltyScore: "", finalResult: true },
  });
  const participantForm = useForm<ParticipantForm>({
    resolver: zodResolver(participantSchema),
    defaultValues: { matchId: "", homeTeamId: "", awayTeamId: "" },
  });

  const upsertResult = useMutation({
    mutationFn: (values: ResultForm) =>
      api.upsertResult(values.matchId, {
        homeScore: values.homeScore,
        awayScore: values.awayScore,
        homePenaltyScore: values.homePenaltyScore === "" ? null : values.homePenaltyScore,
        awayPenaltyScore: values.awayPenaltyScore === "" ? null : values.awayPenaltyScore,
        finalResult: values.finalResult,
      }),
    onSuccess: async (response) => {
      setRecalculation(response);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["matches", tournamentId] }),
        queryClient.invalidateQueries({ queryKey: ["predictions", poolId] }),
        queryClient.invalidateQueries({ queryKey: ["leaderboard", poolId] }),
      ]);
    },
    onError: (error) => resultForm.setError("root", { message: error instanceof Error ? error.message : "Result update failed" }),
  });
  const resolveParticipants = useMutation({
    mutationFn: (values: ParticipantForm) =>
      api.resolveParticipants(values.matchId, {
        homeTeamId: values.homeTeamId,
        awayTeamId: values.awayTeamId,
      }),
    onSuccess: async () => {
      participantForm.reset();
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["matches", tournamentId] }),
        queryClient.invalidateQueries({ queryKey: ["predictions", poolId] }),
      ]);
    },
    onError: (error) =>
      participantForm.setError("root", { message: error instanceof Error ? error.message : "Participant resolution failed" }),
  });

  const matches = matchesQuery.data ?? [];
  const allMatches = allMatchesQuery.data ?? matches;
  const teams = useMemo(() => uniqueTeams(allMatches), [allMatches]);

  function draftFor(match: MatchSummary) {
    const existing = myPredictions.get(match.matchId);
    const draft = drafts[match.matchId];
    return {
      homeScore: draft?.homeScore ?? String(existing?.homeScore ?? ""),
      awayScore: draft?.awayScore ?? String(existing?.awayScore ?? ""),
    };
  }

  function updateDraft(matchId: string, key: "homeScore" | "awayScore", value: string) {
    setDrafts((current) => ({
      ...current,
      [matchId]: {
        homeScore: current[matchId]?.homeScore ?? "",
        awayScore: current[matchId]?.awayScore ?? "",
        [key]: value,
      },
    }));
  }

  function savePrediction(match: MatchSummary) {
    const draft = draftFor(match);
    const homeScore = Number(draft.homeScore);
    const awayScore = Number(draft.awayScore);
    if (!Number.isInteger(homeScore) || !Number.isInteger(awayScore) || homeScore < 0 || awayScore < 0) {
      setSuccessMessage(t("invalidScores"));
      return;
    }
    submitPrediction.mutate({ matchId: match.matchId, homeScore, awayScore });
  }

  if (poolQuery.isLoading) {
    return <p className="text-sm text-muted-foreground">{t("loadingPool")}</p>;
  }

  if (poolQuery.isError || !poolQuery.data) {
    return (
      <Alert className="border-destructive/30">
        <AlertTitle>{t("poolUnavailable")}</AlertTitle>
        <AlertDescription>{poolQuery.error instanceof Error ? poolQuery.error.message : t("couldNotLoadPool")}</AlertDescription>
      </Alert>
    );
  }

  const pool = poolQuery.data;
  const isAdmin = user?.role === "ADMIN";

  return (
    <div className="space-y-6">
      <Button asChild variant="ghost" size="sm">
        <Link to="/">
          <ArrowLeft className="h-4 w-4" />
          {t("dashboard")}
        </Link>
      </Button>

      <section className="flex flex-col gap-4 rounded-lg border bg-white p-5 shadow-sm lg:flex-row lg:items-start lg:justify-between">
        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold">{pool.name}</h1>
            <Badge variant={pool.membershipRole === "OWNER" ? "success" : "secondary"}>{pool.membershipRole}</Badge>
          </div>
          <p className="max-w-3xl text-sm text-muted-foreground">{pool.description || t("noDescription")}</p>
          <div className="flex flex-wrap gap-2 text-sm">
            <Badge variant="outline" className="gap-1">
              <Clipboard className="h-3 w-3" />
              {pool.inviteCode}
            </Badge>
            <Badge variant="outline">{t("tournament")} {pool.tournamentId.slice(0, 8)}</Badge>
          </div>
        </div>
        {isAdmin ? (
          <Badge variant="warning" className="gap-1">
            <Shield className="h-3 w-3" />
            {t("tournamentAdminAvailable")}
          </Badge>
        ) : (
          <p className="max-w-sm text-sm text-muted-foreground">
            {t("poolAdminHidden")}
          </p>
        )}
      </section>

      <Tabs defaultValue="matches">
        <TabsList className="w-full justify-start overflow-x-auto sm:w-auto">
          <TabsTrigger value="matches">{t("matches")}</TabsTrigger>
          <TabsTrigger value="predictions">{t("predictions")}</TabsTrigger>
          <TabsTrigger value="leaderboard">{t("leaderboard")}</TabsTrigger>
          {isAdmin ? <TabsTrigger value="admin">{t("tournamentAdmin")}</TabsTrigger> : null}
        </TabsList>

        <TabsContent value="matches">
          <div className="space-y-4">
            <MatchFilters filters={filters} teams={teams} onChange={setFilters} />
            {successMessage ? (
              <Alert>
                <AlertDescription>{successMessage}</AlertDescription>
              </Alert>
            ) : null}
            <Card>
              <CardHeader>
                <CardTitle>{t("matchesAndPredictions")}</CardTitle>
                <CardDescription>{t("matchesAndPredictionsDesc")}</CardDescription>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{t("match")}</TableHead>
                      <TableHead>{t("kickoff")}</TableHead>
                      <TableHead>{t("status")}</TableHead>
                      <TableHead>{t("result")}</TableHead>
                      <TableHead className="min-w-56">{t("myPrediction")}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {matches.map((match) => {
                      const draft = draftFor(match);
                      return (
                        <TableRow key={match.matchId}>
                          <TableCell>
                            <div className="font-medium">
                              <span className="mr-1">{participantFlag(match, "home")}</span>
                              {participantName(match, "home")} <span className="text-muted-foreground">vs</span>{" "}
                              <span className="mr-1">{participantFlag(match, "away")}</span>
                              {participantName(match, "away")}
                            </div>
                            <div className="mt-1 flex flex-wrap gap-1">
                              <Badge variant="outline">{participantFlag(match, "home")} {participantCode(match, "home")}</Badge>
                              <Badge variant="outline">{participantFlag(match, "away")} {participantCode(match, "away")}</Badge>
                              <Badge variant="secondary">{t("group")} {match.groupName ?? "-"}</Badge>
                              {!hasResolvedParticipants(match) ? <Badge variant="warning">{t("unresolved")}</Badge> : null}
                            </div>
                          </TableCell>
                          <TableCell>{formatDateTime(match.kickoffAt)}</TableCell>
                          <TableCell>
                            <Badge variant={match.predictionOpen ? "success" : "muted"}>
                              {match.predictionOpen ? t("open") : match.status}
                            </Badge>
                          </TableCell>
                          <TableCell>{match.result ? `${match.result.homeScore}-${match.result.awayScore}` : "-"}</TableCell>
                          <TableCell>
                            <div className="flex items-center gap-2">
                              <Input
                                aria-label={`${participantCode(match, "home")} prediction`}
                                type="number"
                                min={0}
                                className="h-9 w-16"
                                value={draft.homeScore}
                                disabled={!match.predictionOpen}
                                onChange={(event) => updateDraft(match.matchId, "homeScore", event.target.value)}
                              />
                              <span className="text-muted-foreground">-</span>
                              <Input
                                aria-label={`${participantCode(match, "away")} prediction`}
                                type="number"
                                min={0}
                                className="h-9 w-16"
                                value={draft.awayScore}
                                disabled={!match.predictionOpen}
                                onChange={(event) => updateDraft(match.matchId, "awayScore", event.target.value)}
                              />
                              <Button size="sm" disabled={!match.predictionOpen || submitPrediction.isPending} onClick={() => savePrediction(match)}>
                                {t("save")}
                              </Button>
                            </div>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
                {matches.length === 0 ? <p className="py-8 text-center text-sm text-muted-foreground">{t("noMatches")}</p> : null}
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="predictions">
          <PredictionsTable predictions={predictionsQuery.data ?? []} />
        </TabsContent>

        <TabsContent value="leaderboard">
          <Card>
            <CardHeader>
              <CardTitle>{t("leaderboard")}</CardTitle>
              <CardDescription>{t("leaderboardDesc")}</CardDescription>
            </CardHeader>
            <CardContent>
              {(leaderboardQuery.data ?? []).length === 0 ? (
                <p className="py-8 text-center text-sm text-muted-foreground">
                  {t("noLeaderboard")}
                </p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{t("rank")}</TableHead>
                      <TableHead>{t("user")}</TableHead>
                      <TableHead>{t("points")}</TableHead>
                      <TableHead>{t("recalculated")}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {leaderboardQuery.data?.map((entry) => (
                      <TableRow key={entry.userId}>
                        <TableCell className="font-semibold">#{entry.rankPosition}</TableCell>
                        <TableCell>{entry.username}</TableCell>
                        <TableCell>{entry.totalPoints}</TableCell>
                        <TableCell>{formatDateTime(entry.recalculatedAt)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {isAdmin ? (
          <TabsContent value="admin">
            <div className="space-y-4">
              <Alert>
                <AlertTitle>{t("tournamentWideOps")}</AlertTitle>
                <AlertDescription>
                  {t("tournamentWideOpsDesc")}
                </AlertDescription>
              </Alert>
              <AdminParticipantsCard matches={matches} teams={teams} form={participantForm} mutation={resolveParticipants} />
              <AdminResultCard matches={matches} form={resultForm} mutation={upsertResult} recalculation={recalculation} />
            </div>
          </TabsContent>
        ) : null}
      </Tabs>
    </div>
  );
}

function MatchFilters({
  filters,
  teams,
  onChange,
}: {
  filters: { group: string; team: string; predictableOnly: boolean };
  teams: TeamSummary[];
  onChange: (filters: { group: string; team: string; predictableOnly: boolean }) => void;
}) {
  const { t } = useLanguage();

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-5 sm:flex-row sm:items-center">
        <Filter className="hidden h-4 w-4 text-muted-foreground sm:block" />
        <Input
          placeholder={t("group")}
          className="sm:w-32"
          value={filters.group}
          onChange={(event) => onChange({ ...filters, group: event.target.value.toUpperCase() })}
        />
        <select
          className="h-10 rounded-md border border-input bg-white px-3 text-sm shadow-sm sm:w-56"
          value={filters.team}
          onChange={(event) => onChange({ ...filters, team: event.target.value })}
        >
          <option value="">{t("selectFlag")}</option>
          {teams.map((team) => (
            <option key={team.id} value={team.fifaCode}>
              {flagForFifaCode(team.fifaCode)} {team.fifaCode} - {team.name}
            </option>
          ))}
        </select>
        <Input
          placeholder={t("teamCode")}
          className="sm:w-48"
          value={filters.team}
          onChange={(event) => onChange({ ...filters, team: event.target.value.toUpperCase() })}
        />
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={filters.predictableOnly}
            onChange={(event) => onChange({ ...filters, predictableOnly: event.target.checked })}
          />
          {t("predictableOnly")}
        </label>
      </CardContent>
    </Card>
  );
}

function PredictionsTable({ predictions }: { predictions: PoolPrediction[] }) {
  const { t } = useLanguage();

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("visiblePredictions")}</CardTitle>
        <CardDescription>{t("visiblePredictionsDesc")}</CardDescription>
      </CardHeader>
      <CardContent>
        {predictions.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">{t("noVisiblePredictions")}</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("user")}</TableHead>
                <TableHead>{t("match")}</TableHead>
                <TableHead>{t("prediction")}</TableHead>
                <TableHead>{t("submitted")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {predictions.map((prediction) => (
                <TableRow key={prediction.predictionId}>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      {prediction.user.username}
                      {prediction.mine ? <Badge variant="success">{t("mine")}</Badge> : null}
                    </div>
                  </TableCell>
                  <TableCell>
                    {participantFlag(prediction.match, "home")} {participantCode(prediction.match, "home")} vs{" "}
                    {participantFlag(prediction.match, "away")} {participantCode(prediction.match, "away")}
                  </TableCell>
                  <TableCell className="font-semibold">
                    {prediction.homeScore}-{prediction.awayScore}
                  </TableCell>
                  <TableCell>{formatDateTime(prediction.submittedAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}

function AdminParticipantsCard({
  matches,
  teams,
  form,
  mutation,
}: {
  matches: MatchSummary[];
  teams: TeamSummary[];
  form: UseFormReturn<ParticipantForm>;
  mutation: UseMutationResult<MatchSummary, Error, ParticipantForm>;
}) {
  const { t } = useLanguage();
  const unresolvedMatches = matches.filter((match) => !hasResolvedParticipants(match));

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("participantResolution")}</CardTitle>
        <CardDescription>{t("participantResolutionDesc")}</CardDescription>
      </CardHeader>
      <CardContent>
        {unresolvedMatches.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t("noUnresolved")}</p>
        ) : (
          <form onSubmit={form.handleSubmit((values) => mutation.mutate(values))} className="grid gap-4 lg:grid-cols-[1.5fr_1fr_1fr_auto] lg:items-end">
            {form.formState.errors.root ? (
              <Alert className="border-destructive/30 lg:col-span-4">
                <AlertDescription>{form.formState.errors.root.message}</AlertDescription>
              </Alert>
            ) : null}
            <FormField label={t("placeholderMatch")} error={form.formState.errors.matchId}>
              <select className="h-10 w-full rounded-md border border-input bg-white px-3 text-sm" {...form.register("matchId")}>
                <option value="">{t("selectMatch")}</option>
                {unresolvedMatches.map((match) => (
                  <option key={match.matchId} value={match.matchId}>
                    {participantCode(match, "home")} vs {participantCode(match, "away")} - {formatDateTime(match.kickoffAt)}
                  </option>
                ))}
              </select>
            </FormField>
            <FormField label={t("homeTeam")} error={form.formState.errors.homeTeamId}>
              <TeamSelect teams={teams} {...form.register("homeTeamId")} />
            </FormField>
            <FormField label={t("awayTeam")} error={form.formState.errors.awayTeamId}>
              <TeamSelect teams={teams} {...form.register("awayTeamId")} />
            </FormField>
            <Button disabled={mutation.isPending || teams.length < 2}>
              <Shield className="h-4 w-4" />
              {t("resolve")}
            </Button>
          </form>
        )}
      </CardContent>
    </Card>
  );
}

function TeamSelect({
  teams,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement> & { teams: TeamSummary[] }) {
  const { t } = useLanguage();

  return (
    <select className="h-10 w-full rounded-md border border-input bg-white px-3 text-sm" {...props}>
      <option value="">{t("selectTeam")}</option>
      {teams.map((team) => (
        <option key={team.id} value={team.id}>
          {flagForFifaCode(team.fifaCode)} {team.fifaCode} - {team.name}
        </option>
      ))}
    </select>
  );
}

function AdminResultCard({
  matches,
  form,
  mutation,
  recalculation,
}: {
  matches: MatchSummary[];
  form: UseFormReturn<ResultForm>;
  mutation: UseMutationResult<RecalculationResponse, Error, ResultForm>;
  recalculation: RecalculationResponse | null;
}) {
  const { t } = useLanguage();
  const resolvedMatches = matches.filter(hasResolvedParticipants);

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("resultUpdate")}</CardTitle>
        <CardDescription>{t("resultUpdateDesc")}</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={form.handleSubmit((values) => mutation.mutate(values))} className="grid gap-4 lg:grid-cols-[1.6fr_repeat(4,0.6fr)_auto] lg:items-end">
          {form.formState.errors.root ? (
            <Alert className="border-destructive/30 lg:col-span-6">
              <AlertDescription>{form.formState.errors.root.message}</AlertDescription>
            </Alert>
          ) : null}
          <FormField label={t("match")} error={form.formState.errors.matchId}>
            <select className="h-10 w-full rounded-md border border-input bg-white px-3 text-sm" {...form.register("matchId")}>
              <option value="">{t("selectMatch")}</option>
              {resolvedMatches.map((match) => (
                <option key={match.matchId} value={match.matchId}>
                  {participantCode(match, "home")} vs {participantCode(match, "away")} - {formatDateTime(match.kickoffAt)}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label={t("home")} error={form.formState.errors.homeScore}>
            <Input type="number" min={0} {...form.register("homeScore")} />
          </FormField>
          <FormField label={t("away")} error={form.formState.errors.awayScore}>
            <Input type="number" min={0} {...form.register("awayScore")} />
          </FormField>
          <FormField label={t("homePens")} error={form.formState.errors.homePenaltyScore}>
            <Input type="number" min={0} {...form.register("homePenaltyScore")} />
          </FormField>
          <FormField label={t("awayPens")} error={form.formState.errors.awayPenaltyScore}>
            <Input type="number" min={0} {...form.register("awayPenaltyScore")} />
          </FormField>
          <Button disabled={mutation.isPending}>
            <RefreshCw className="h-4 w-4" />
            {t("recalculate")}
          </Button>
          <label className="flex items-center gap-2 text-sm lg:col-span-6">
            <input type="checkbox" {...form.register("finalResult")} />
            {t("finalResult")}
          </label>
        </form>
        {recalculation ? (
          <Alert className="mt-4">
            <CheckCircle2 className="mr-2 inline h-4 w-4 text-emerald-700" />
            <AlertTitle className="inline">{t("recalculationComplete")}</AlertTitle>
            <AlertDescription className="mt-2 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
              <span>{t("scoredPredictions")}: {recalculation.scoredPredictions}</span>
              <span>{t("affectedPools")}: {recalculation.affectedPools}</span>
              <span>{t("idempotentReplay")}: {String(recalculation.idempotentReplay)}</span>
              <span className="font-mono">{t("checksum")}: {recalculation.resultChecksum}</span>
            </AlertDescription>
          </Alert>
        ) : null}
      </CardContent>
    </Card>
  );
}

function hasResolvedParticipants(match: MatchSummary) {
  return Boolean(match.homeTeam && match.awayTeam);
}

function participantName(match: MatchSummary, side: "home" | "away") {
  const team = side === "home" ? match.homeTeam : match.awayTeam;
  const placeholder = side === "home" ? match.homePlaceholder : match.awayPlaceholder;
  return team?.name ?? `Placeholder ${placeholder ?? "-"}`;
}

function participantCode(match: MatchSummary, side: "home" | "away") {
  const team = side === "home" ? match.homeTeam : match.awayTeam;
  const placeholder = side === "home" ? match.homePlaceholder : match.awayPlaceholder;
  return team?.fifaCode ?? placeholder ?? "TBD";
}

function participantFlag(match: MatchSummary, side: "home" | "away") {
  const team = side === "home" ? match.homeTeam : match.awayTeam;
  return team ? flagForFifaCode(team.fifaCode) : "🏳️";
}

function uniqueTeams(matches: MatchSummary[]) {
  const byId = new Map<string, TeamSummary>();
  for (const match of matches) {
    if (match.homeTeam) byId.set(match.homeTeam.id, match.homeTeam);
    if (match.awayTeam) byId.set(match.awayTeam.id, match.awayTeam);
  }
  return [...byId.values()].sort((a, b) => a.fifaCode.localeCompare(b.fifaCode));
}
