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
      setSuccessMessage("Prediction saved");
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
  const teams = useMemo(() => uniqueTeams(matches), [matches]);

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
      setSuccessMessage("Scores must be non-negative whole numbers");
      return;
    }
    submitPrediction.mutate({ matchId: match.matchId, homeScore, awayScore });
  }

  if (poolQuery.isLoading) {
    return <p className="text-sm text-muted-foreground">Loading pool...</p>;
  }

  if (poolQuery.isError || !poolQuery.data) {
    return (
      <Alert className="border-destructive/30">
        <AlertTitle>Pool unavailable</AlertTitle>
        <AlertDescription>{poolQuery.error instanceof Error ? poolQuery.error.message : "Could not load pool"}</AlertDescription>
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
          Dashboard
        </Link>
      </Button>

      <section className="flex flex-col gap-4 rounded-lg border bg-white p-5 shadow-sm lg:flex-row lg:items-start lg:justify-between">
        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold">{pool.name}</h1>
            <Badge variant={pool.membershipRole === "OWNER" ? "success" : "secondary"}>{pool.membershipRole}</Badge>
          </div>
          <p className="max-w-3xl text-sm text-muted-foreground">{pool.description || "No description provided."}</p>
          <div className="flex flex-wrap gap-2 text-sm">
            <Badge variant="outline" className="gap-1">
              <Clipboard className="h-3 w-3" />
              {pool.inviteCode}
            </Badge>
            <Badge variant="outline">Tournament {pool.tournamentId.slice(0, 8)}</Badge>
          </div>
        </div>
        {isAdmin ? (
          <Badge variant="warning" className="gap-1">
            <Shield className="h-3 w-3" />
            Tournament admin available
          </Badge>
        ) : (
          <p className="max-w-sm text-sm text-muted-foreground">
            Tournament-wide result and participant updates are hidden because this account is not an ADMIN user.
          </p>
        )}
      </section>

      <Tabs defaultValue="matches">
        <TabsList className="w-full justify-start overflow-x-auto sm:w-auto">
          <TabsTrigger value="matches">Matches</TabsTrigger>
          <TabsTrigger value="predictions">Predictions</TabsTrigger>
          <TabsTrigger value="leaderboard">Leaderboard</TabsTrigger>
          {isAdmin ? <TabsTrigger value="admin">Tournament Admin</TabsTrigger> : null}
        </TabsList>

        <TabsContent value="matches">
          <div className="space-y-4">
            <MatchFilters filters={filters} onChange={setFilters} />
            {successMessage ? (
              <Alert>
                <AlertDescription>{successMessage}</AlertDescription>
              </Alert>
            ) : null}
            <Card>
              <CardHeader>
                <CardTitle>Matches and predictions</CardTitle>
                <CardDescription>Predictions close at kickoff according to backend match time.</CardDescription>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Match</TableHead>
                      <TableHead>Kickoff</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Result</TableHead>
                      <TableHead className="min-w-56">My prediction</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {matches.map((match) => {
                      const draft = draftFor(match);
                      return (
                        <TableRow key={match.matchId}>
                          <TableCell>
                            <div className="font-medium">
                              {participantName(match, "home")} <span className="text-muted-foreground">vs</span>{" "}
                              {participantName(match, "away")}
                            </div>
                            <div className="mt-1 flex flex-wrap gap-1">
                              <Badge variant="outline">{participantCode(match, "home")}</Badge>
                              <Badge variant="outline">{participantCode(match, "away")}</Badge>
                              <Badge variant="secondary">Group {match.groupName ?? "-"}</Badge>
                              {!hasResolvedParticipants(match) ? <Badge variant="warning">Unresolved</Badge> : null}
                            </div>
                          </TableCell>
                          <TableCell>{formatDateTime(match.kickoffAt)}</TableCell>
                          <TableCell>
                            <Badge variant={match.predictionOpen ? "success" : "muted"}>
                              {match.predictionOpen ? "Open" : match.status}
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
                                Save
                              </Button>
                            </div>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
                {matches.length === 0 ? <p className="py-8 text-center text-sm text-muted-foreground">No matches match these filters.</p> : null}
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
              <CardTitle>Leaderboard</CardTitle>
              <CardDescription>Ranks are rebuilt by the backend when official results are upserted.</CardDescription>
            </CardHeader>
            <CardContent>
              {(leaderboardQuery.data ?? []).length === 0 ? (
                <p className="py-8 text-center text-sm text-muted-foreground">
                  No leaderboard entries yet. Submit predictions and record official results to populate standings.
                </p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Rank</TableHead>
                      <TableHead>User</TableHead>
                      <TableHead>Points</TableHead>
                      <TableHead>Recalculated</TableHead>
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
                <AlertTitle>Tournament-wide operations</AlertTitle>
                <AlertDescription>
                  Official match results and participant resolutions apply to every pool using this tournament. Recalculation
                  reports how many predictions were scored and how many pools were affected.
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
  onChange,
}: {
  filters: { group: string; team: string; predictableOnly: boolean };
  onChange: (filters: { group: string; team: string; predictableOnly: boolean }) => void;
}) {
  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-5 sm:flex-row sm:items-center">
        <Filter className="hidden h-4 w-4 text-muted-foreground sm:block" />
        <Input
          placeholder="Group"
          className="sm:w-32"
          value={filters.group}
          onChange={(event) => onChange({ ...filters, group: event.target.value.toUpperCase() })}
        />
        <Input
          placeholder="Team FIFA code"
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
          Predictable only
        </label>
      </CardContent>
    </Card>
  );
}

function PredictionsTable({ predictions }: { predictions: PoolPrediction[] }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Visible predictions</CardTitle>
        <CardDescription>Other members' predictions appear only after the match closes.</CardDescription>
      </CardHeader>
      <CardContent>
        {predictions.length === 0 ? (
          <p className="py-8 text-center text-sm text-muted-foreground">No visible predictions yet.</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>User</TableHead>
                <TableHead>Match</TableHead>
                <TableHead>Prediction</TableHead>
                <TableHead>Submitted</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {predictions.map((prediction) => (
                <TableRow key={prediction.predictionId}>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      {prediction.user.username}
                      {prediction.mine ? <Badge variant="success">Mine</Badge> : null}
                    </div>
                  </TableCell>
                  <TableCell>
                    {participantCode(prediction.match, "home")} vs {participantCode(prediction.match, "away")}
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
  const unresolvedMatches = matches.filter((match) => !hasResolvedParticipants(match));

  return (
    <Card>
      <CardHeader>
        <CardTitle>Tournament participant resolution</CardTitle>
        <CardDescription>Resolve placeholder knockout participants for this tournament before predictions and official results can open.</CardDescription>
      </CardHeader>
      <CardContent>
        {unresolvedMatches.length === 0 ? (
          <p className="text-sm text-muted-foreground">No unresolved matches in the current match set.</p>
        ) : (
          <form onSubmit={form.handleSubmit((values) => mutation.mutate(values))} className="grid gap-4 lg:grid-cols-[1.5fr_1fr_1fr_auto] lg:items-end">
            {form.formState.errors.root ? (
              <Alert className="border-destructive/30 lg:col-span-4">
                <AlertDescription>{form.formState.errors.root.message}</AlertDescription>
              </Alert>
            ) : null}
            <FormField label="Placeholder match" error={form.formState.errors.matchId}>
              <select className="h-10 w-full rounded-md border border-input bg-white px-3 text-sm" {...form.register("matchId")}>
                <option value="">Select match</option>
                {unresolvedMatches.map((match) => (
                  <option key={match.matchId} value={match.matchId}>
                    {participantCode(match, "home")} vs {participantCode(match, "away")} - {formatDateTime(match.kickoffAt)}
                  </option>
                ))}
              </select>
            </FormField>
            <FormField label="Home team" error={form.formState.errors.homeTeamId}>
              <TeamSelect teams={teams} {...form.register("homeTeamId")} />
            </FormField>
            <FormField label="Away team" error={form.formState.errors.awayTeamId}>
              <TeamSelect teams={teams} {...form.register("awayTeamId")} />
            </FormField>
            <Button disabled={mutation.isPending || teams.length < 2}>
              <Shield className="h-4 w-4" />
              Resolve
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
  return (
    <select className="h-10 w-full rounded-md border border-input bg-white px-3 text-sm" {...props}>
      <option value="">Select team</option>
      {teams.map((team) => (
        <option key={team.id} value={team.id}>
          {team.fifaCode} - {team.name}
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
  const resolvedMatches = matches.filter(hasResolvedParticipants);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Tournament result update</CardTitle>
        <CardDescription>Upsert an official tournament result and inspect the idempotent recalculation response across affected pools.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={form.handleSubmit((values) => mutation.mutate(values))} className="grid gap-4 lg:grid-cols-[1.6fr_repeat(4,0.6fr)_auto] lg:items-end">
          {form.formState.errors.root ? (
            <Alert className="border-destructive/30 lg:col-span-6">
              <AlertDescription>{form.formState.errors.root.message}</AlertDescription>
            </Alert>
          ) : null}
          <FormField label="Match" error={form.formState.errors.matchId}>
            <select className="h-10 w-full rounded-md border border-input bg-white px-3 text-sm" {...form.register("matchId")}>
              <option value="">Select match</option>
              {resolvedMatches.map((match) => (
                <option key={match.matchId} value={match.matchId}>
                  {participantCode(match, "home")} vs {participantCode(match, "away")} - {formatDateTime(match.kickoffAt)}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Home" error={form.formState.errors.homeScore}>
            <Input type="number" min={0} {...form.register("homeScore")} />
          </FormField>
          <FormField label="Away" error={form.formState.errors.awayScore}>
            <Input type="number" min={0} {...form.register("awayScore")} />
          </FormField>
          <FormField label="Home pens" error={form.formState.errors.homePenaltyScore}>
            <Input type="number" min={0} {...form.register("homePenaltyScore")} />
          </FormField>
          <FormField label="Away pens" error={form.formState.errors.awayPenaltyScore}>
            <Input type="number" min={0} {...form.register("awayPenaltyScore")} />
          </FormField>
          <Button disabled={mutation.isPending}>
            <RefreshCw className="h-4 w-4" />
            Recalculate
          </Button>
          <label className="flex items-center gap-2 text-sm lg:col-span-6">
            <input type="checkbox" {...form.register("finalResult")} />
            Final result
          </label>
        </form>
        {recalculation ? (
          <Alert className="mt-4">
            <CheckCircle2 className="mr-2 inline h-4 w-4 text-emerald-700" />
            <AlertTitle className="inline">Recalculation complete</AlertTitle>
            <AlertDescription className="mt-2 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
              <span>Scored predictions: {recalculation.scoredPredictions}</span>
              <span>Affected pools: {recalculation.affectedPools}</span>
              <span>Idempotent replay: {String(recalculation.idempotentReplay)}</span>
              <span className="font-mono">Checksum: {recalculation.resultChecksum}</span>
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

function uniqueTeams(matches: MatchSummary[]) {
  const byId = new Map<string, TeamSummary>();
  for (const match of matches) {
    if (match.homeTeam) byId.set(match.homeTeam.id, match.homeTeam);
    if (match.awayTeam) byId.set(match.awayTeam.id, match.awayTeam);
  }
  return [...byId.values()].sort((a, b) => a.fifaCode.localeCompare(b.fifaCode));
}
