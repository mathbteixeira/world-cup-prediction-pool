import { useMemo, useState, type Dispatch, type SetStateAction } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient, type UseMutationResult } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, type UseFormReturn } from "react-hook-form";
import { z } from "zod";
import { ArrowLeft, ArrowUpDown, CheckCircle2, Clipboard, Filter, ListOrdered, Trash2, TriangleAlert, Trophy, UserPlus } from "lucide-react";
import { ApiError, api, WORLD_CUP_2026_TOURNAMENT_ID } from "../api/client";
import type {
  GroupStandingResponse,
  ManagedParticipant,
  MatchSummary,
  PoolPrediction,
  PredictionResponse,
  TeamSummary,
  TopScorerPick,
  TopScorerResponse,
  TournamentRankingPicks,
  TournamentRankingResponse,
} from "../api/types";
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
type ManagedScoreDraft = Record<string, { homeScore: string; awayScore: string }>;
type GroupStandingDraft = Record<string, string[]>;
type FinalRankingDraft = Record<keyof TournamentRankingPicks, string>;
type TopScorerDraft = {
  teamId: string;
  playerName: string;
  goals: string;
};
type MatchSortDirection = "asc" | "desc";
type MatchRound = "GROUP_STAGE" | "ROUND_OF_32" | "ROUND_OF_16" | "QUARTER_FINAL" | "SEMI_FINAL" | "THIRD_PLACE" | "FINAL";
type MatchFiltersState = {
  group: string;
  round: string;
  teams: string[];
  predictableOnly: boolean;
  sortDirection: MatchSortDirection;
};
type FeedbackMessage = { type: "success" | "error"; message: string } | null;

const managedParticipantSchema = z.object({
  name: z.string().trim().min(1, "Name is required").max(80),
});

type ManagedParticipantForm = z.infer<typeof managedParticipantSchema>;

const MATCH_ROUNDS: MatchRound[] = [
  "GROUP_STAGE",
  "ROUND_OF_32",
  "ROUND_OF_16",
  "QUARTER_FINAL",
  "SEMI_FINAL",
  "THIRD_PLACE",
  "FINAL",
];

const TOURNAMENT_PREDICTION_FRONTEND_DEADLINE = new Date("2026-06-21T00:00:00");

export function PoolDetailPage() {
  const { poolId = "" } = useParams();
  const { language, t } = useLanguage();
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<MatchFiltersState>({
    group: "",
    round: "",
    teams: [],
    predictableOnly: false,
    sortDirection: "asc",
  });
  const [drafts, setDrafts] = useState<ScoreDraft>({});
  const [managedDrafts, setManagedDrafts] = useState<ManagedScoreDraft>({});
  const [groupStandingDrafts, setGroupStandingDrafts] = useState<GroupStandingDraft>({});
  const [finalRankingDraft, setFinalRankingDraft] = useState<Partial<FinalRankingDraft>>({});
  const [topScorerDraft, setTopScorerDraft] = useState<Partial<TopScorerDraft>>({});
  const [feedback, setFeedback] = useState<FeedbackMessage>(null);

  const poolQuery = useQuery({ queryKey: ["pool", poolId], queryFn: () => api.getPool(poolId), enabled: Boolean(poolId) });
  const tournamentId = poolQuery.data?.tournamentId;
  const singleMatchId = poolQuery.data?.singleMatchId;
  const allMatchesQuery = useQuery({
    queryKey: ["matches", tournamentId, singleMatchId ?? "all"],
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
  const managedParticipantsQuery = useQuery({
    queryKey: ["managed-participants", poolId],
    queryFn: () => api.listManagedParticipants(poolId),
    enabled: Boolean(poolId) && poolQuery.data?.poolScope === "SINGLE_MATCH" && poolQuery.data?.membershipRole === "OWNER",
  });
  const groupStandingsQuery = useQuery({
    queryKey: ["group-standings", poolId],
    queryFn: () => api.listGroupStandings(poolId),
    enabled: Boolean(poolId) && poolQuery.data?.poolScope === "TOURNAMENT",
  });
  const finalRankingQuery = useQuery({
    queryKey: ["final-ranking", poolId],
    queryFn: () => api.getFinalRanking(poolId),
    enabled: Boolean(poolId) && poolQuery.data?.poolScope === "TOURNAMENT",
  });
  const topScorerQuery = useQuery({
    queryKey: ["top-scorer", poolId],
    queryFn: () => api.getTopScorer(poolId),
    enabled: Boolean(poolId) && poolQuery.data?.poolScope === "TOURNAMENT",
  });

  const myPredictions = useMemo(() => {
    const map = new Map<string, PoolPrediction>();
    for (const prediction of predictionsQuery.data ?? []) {
      if (prediction.mine) map.set(prediction.match.matchId, prediction);
    }
    return map;
  }, [predictionsQuery.data]);
  const managedPredictions = useMemo(() => {
    const map = new Map<string, PoolPrediction>();
    for (const prediction of predictionsQuery.data ?? []) {
      if (prediction.managedParticipant) map.set(prediction.managedParticipant.participantId, prediction);
    }
    return map;
  }, [predictionsQuery.data]);

  const submitPrediction = useMutation({
    mutationFn: ({ matchId, homeScore, awayScore }: { matchId: string; homeScore: number; awayScore: number }) =>
      api.submitPrediction(poolId, matchId, { homeScore, awayScore }),
    onSuccess: async () => {
      setFeedback({ type: "success", message: t("predictionSaved") });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["predictions", poolId] }),
        queryClient.invalidateQueries({ queryKey: ["leaderboard", poolId] }),
      ]);
    },
    onError: (error) => setFeedback({ type: "error", message: predictionErrorMessage(error, t) }),
  });

  const managedParticipantForm = useForm<ManagedParticipantForm>({
    resolver: zodResolver(managedParticipantSchema),
    defaultValues: { name: "" },
  });

  const createManagedParticipant = useMutation({
    mutationFn: (values: ManagedParticipantForm) => api.createManagedParticipant(poolId, { name: values.name.trim() }),
    onSuccess: async () => {
      managedParticipantForm.reset();
      await queryClient.invalidateQueries({ queryKey: ["managed-participants", poolId] });
    },
    onError: (error) =>
      managedParticipantForm.setError("root", { message: error instanceof Error ? error.message : "Participant creation failed" }),
  });
  const removeManagedParticipant = useMutation({
    mutationFn: (participantId: string) => api.removeManagedParticipant(poolId, participantId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["managed-participants", poolId] }),
        queryClient.invalidateQueries({ queryKey: ["predictions", poolId] }),
        queryClient.invalidateQueries({ queryKey: ["leaderboard", poolId] }),
      ]);
    },
  });
  const submitManagedPrediction = useMutation({
    mutationFn: ({ participantId, homeScore, awayScore }: { participantId: string; homeScore: number; awayScore: number }) =>
      api.submitManagedParticipantPrediction(poolId, participantId, { homeScore, awayScore }),
    onSuccess: async () => {
      setFeedback({ type: "success", message: t("predictionSaved") });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["predictions", poolId] }),
        queryClient.invalidateQueries({ queryKey: ["leaderboard", poolId] }),
      ]);
    },
    onError: (error) => setFeedback({ type: "error", message: predictionErrorMessage(error, t) }),
  });
  const submitGroupStandingPrediction = useMutation({
    mutationFn: ({ groupName, teamIdsByPosition }: { groupName: string; teamIdsByPosition: string[] }) =>
      api.submitGroupStandingPrediction(poolId, groupName, teamIdsByPosition),
    onSuccess: async () => {
      setFeedback({ type: "success", message: t("predictionSaved") });
      await queryClient.invalidateQueries({ queryKey: ["group-standings", poolId] });
    },
    onError: (error) => setFeedback({ type: "error", message: predictionErrorMessage(error, t) }),
  });
  const submitFinalRankingPrediction = useMutation({
    mutationFn: (picks: TournamentRankingPicks) => api.submitFinalRankingPrediction(poolId, picks),
    onSuccess: async () => {
      setFeedback({ type: "success", message: t("predictionSaved") });
      await queryClient.invalidateQueries({ queryKey: ["final-ranking", poolId] });
    },
    onError: (error) => setFeedback({ type: "error", message: predictionErrorMessage(error, t) }),
  });
  const submitTopScorerPrediction = useMutation({
    mutationFn: (pick: TopScorerPick) => api.submitTopScorerPrediction(poolId, pick),
    onSuccess: async () => {
      setFeedback({ type: "success", message: t("predictionSaved") });
      await queryClient.invalidateQueries({ queryKey: ["top-scorer", poolId] });
    },
    onError: (error) => setFeedback({ type: "error", message: predictionErrorMessage(error, t) }),
  });

  const allMatches = useMemo(() => {
    const matches = allMatchesQuery.data ?? [];
    return singleMatchId ? matches.filter((match) => match.matchId === singleMatchId) : matches;
  }, [allMatchesQuery.data, singleMatchId]);
  const teams = useMemo(() => uniqueTeams(allMatches), [allMatches]);
  const groups = useMemo(() => uniqueGroups(allMatches), [allMatches]);
  const matches = useMemo(() => filterAndSortMatches(allMatches, filters), [allMatches, filters]);

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
    if (!match.predictionOpen) {
      setFeedback({ type: "error", message: t("predictionsClosed") });
      return;
    }
    const draft = draftFor(match);
    const homeScore = Number(draft.homeScore);
    const awayScore = Number(draft.awayScore);
    if (!Number.isInteger(homeScore) || !Number.isInteger(awayScore) || homeScore < 0 || awayScore < 0) {
      setFeedback({ type: "error", message: t("invalidScores") });
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
  const tournamentName = pool.tournamentId === WORLD_CUP_2026_TOURNAMENT_ID ? t("worldCup2026") : pool.tournamentId.slice(0, 8);
  const scopeLabel = pool.poolScope === "SINGLE_MATCH" ? t("singleMatchPool") : t("tournamentPool");
  const canManageParticipants = pool.poolScope === "SINGLE_MATCH" && pool.membershipRole === "OWNER";
  const hasTournamentPredictions = pool.poolScope === "TOURNAMENT";
  const tournamentPredictionOpen = isTournamentPredictionOpen();

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
            <Badge variant="outline">{scopeLabel}</Badge>
            <Badge variant="outline">{t("tournament")} {tournamentName}</Badge>
          </div>
        </div>
      </section>

      <Tabs defaultValue="matches">
        <TabsList className="w-full justify-start overflow-x-auto sm:w-auto">
          <TabsTrigger value="matches">{t("matches")}</TabsTrigger>
          {hasTournamentPredictions ? <TabsTrigger value="tournament-predictions">{t("tournamentPredictions")}</TabsTrigger> : null}
          <TabsTrigger value="predictions">{t("predictions")}</TabsTrigger>
          <TabsTrigger value="leaderboard">{t("leaderboard")}</TabsTrigger>
          {canManageParticipants ? <TabsTrigger value="managed">{t("managedParticipants")}</TabsTrigger> : null}
        </TabsList>
        {feedback ? <FeedbackAlert feedback={feedback} /> : null}

        <TabsContent value="matches">
          <div className="space-y-4">
            <MatchFilters filters={filters} groups={groups} teams={teams} onChange={setFilters} />
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
                      <TableHead>
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-8 px-2"
                          onClick={() =>
                            setFilters((current) => ({
                              ...current,
                              sortDirection: current.sortDirection === "asc" ? "desc" : "asc",
                            }))
                          }
                        >
                          <ArrowUpDown className="h-4 w-4" />
                          {filters.sortDirection === "asc" ? t("kickoffSortAsc") : t("kickoffSortDesc")}
                        </Button>
                      </TableHead>
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
                          <TableCell>{formatDateTime(match.kickoffAt, language)}</TableCell>
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
                              <Button size="sm" disabled={submitPrediction.isPending} onClick={() => savePrediction(match)}>
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

        {hasTournamentPredictions ? (
          <TabsContent value="tournament-predictions">
            <TournamentPredictionsPanel
              groupStandings={groupStandingsQuery.data ?? []}
              finalRanking={finalRankingQuery.data}
              topScorer={topScorerQuery.data}
              groupDrafts={groupStandingDrafts}
              setGroupDrafts={setGroupStandingDrafts}
              finalDraft={finalRankingDraft}
              setFinalDraft={setFinalRankingDraft}
              topScorerDraft={topScorerDraft}
              setTopScorerDraft={setTopScorerDraft}
              canSubmit={tournamentPredictionOpen}
              submitGroupMutation={submitGroupStandingPrediction}
              submitFinalMutation={submitFinalRankingPrediction}
              submitTopScorerMutation={submitTopScorerPrediction}
              setFeedback={setFeedback}
            />
          </TabsContent>
        ) : null}

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
                      <TableRow key={entry.userId ?? entry.managedParticipantId ?? entry.username}>
                        <TableCell className="font-semibold">#{entry.rankPosition}</TableCell>
                        <TableCell>{entry.username}</TableCell>
                        <TableCell>{entry.totalPoints}</TableCell>
                        <TableCell>{formatDateTime(entry.recalculatedAt, language)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {canManageParticipants ? (
          <TabsContent value="managed">
            <ManagedParticipantsCard
              participants={managedParticipantsQuery.data ?? []}
              match={allMatches[0]}
              predictions={managedPredictions}
              form={managedParticipantForm}
              createMutation={createManagedParticipant}
              removeMutation={removeManagedParticipant}
              submitMutation={submitManagedPrediction}
              drafts={managedDrafts}
              setDrafts={setManagedDrafts}
              setFeedback={setFeedback}
            />
          </TabsContent>
        ) : null}

      </Tabs>
    </div>
  );
}

function TournamentPredictionsPanel({
  groupStandings,
  finalRanking,
  topScorer,
  groupDrafts,
  setGroupDrafts,
  finalDraft,
  setFinalDraft,
  topScorerDraft,
  setTopScorerDraft,
  canSubmit,
  submitGroupMutation,
  submitFinalMutation,
  submitTopScorerMutation,
  setFeedback,
}: {
  groupStandings: GroupStandingResponse[];
  finalRanking: TournamentRankingResponse | undefined;
  topScorer: TopScorerResponse | undefined;
  groupDrafts: GroupStandingDraft;
  setGroupDrafts: Dispatch<SetStateAction<GroupStandingDraft>>;
  finalDraft: Partial<FinalRankingDraft>;
  setFinalDraft: Dispatch<SetStateAction<Partial<FinalRankingDraft>>>;
  topScorerDraft: Partial<TopScorerDraft>;
  setTopScorerDraft: Dispatch<SetStateAction<Partial<TopScorerDraft>>>;
  canSubmit: boolean;
  submitGroupMutation: UseMutationResult<GroupStandingResponse, Error, { groupName: string; teamIdsByPosition: string[] }>;
  submitFinalMutation: UseMutationResult<TournamentRankingResponse, Error, TournamentRankingPicks>;
  submitTopScorerMutation: UseMutationResult<TopScorerResponse, Error, TopScorerPick>;
  setFeedback: Dispatch<SetStateAction<FeedbackMessage>>;
}) {
  const { language, t } = useLanguage();
  const orderedGroups = [...groupStandings].sort((a, b) => a.groupName.localeCompare(b.groupName));
  const finalPicks = finalRankingDraftValues(finalRanking, finalDraft);
  const topScorerPick = topScorerDraftValues(topScorer, topScorerDraft);

  function groupDraft(group: GroupStandingResponse) {
    return groupDrafts[group.groupName] ?? group.predictedTeamIdsByPosition ?? ["", "", "", ""];
  }

  function updateGroupDraft(groupName: string, position: number, teamId: string) {
    setGroupDrafts((current) => {
      const next = [...(current[groupName] ?? groupStandings.find((group) => group.groupName === groupName)?.predictedTeamIdsByPosition ?? ["", "", "", ""])];
      next[position] = teamId;
      return { ...current, [groupName]: next };
    });
  }

  function saveGroup(group: GroupStandingResponse) {
    if (!canSubmit || !group.predictionOpen) {
      setFeedback({ type: "error", message: t("tournamentPredictionsClosed") });
      return;
    }
    const picks = groupDraft(group);
    if (!hasCompleteDistinctPicks(picks, 4)) {
      setFeedback({ type: "error", message: t("selectDistinctTeams") });
      return;
    }
    submitGroupMutation.mutate({ groupName: group.groupName, teamIdsByPosition: picks });
  }

  function updateFinalDraft(field: keyof TournamentRankingPicks, teamId: string) {
    setFinalDraft((current) => ({ ...current, [field]: teamId }));
  }

  function saveFinalRanking() {
    if (!finalRanking || !canSubmit || !finalRanking.predictionOpen) {
      setFeedback({ type: "error", message: t("tournamentPredictionsClosed") });
      return;
    }
    const picks = finalRankingDraftValues(finalRanking, finalDraft);
    const values = Object.values(picks);
    if (!hasCompleteDistinctPicks(values, 4)) {
      setFeedback({ type: "error", message: t("selectDistinctTeams") });
      return;
    }
    submitFinalMutation.mutate(picks);
  }

  function updateTopScorerDraft(field: keyof TopScorerDraft, value: string) {
    setTopScorerDraft((current) => ({
      ...current,
      [field]: value,
    }));
  }

  function saveTopScorer() {
    if (!topScorer || !canSubmit || !topScorer.predictionOpen) {
      setFeedback({ type: "error", message: t("tournamentPredictionsClosed") });
      return;
    }
    const goals = Number(topScorerPick.goals);
    if (!topScorerPick.teamId || !topScorerPick.playerName.trim() || !Number.isInteger(goals) || goals < 1 || goals > 15) {
      setFeedback({ type: "error", message: t("selectTopScorer") });
      return;
    }
    submitTopScorerMutation.mutate({
      teamId: topScorerPick.teamId,
      playerName: topScorerPick.playerName.trim(),
      goals,
    });
  }

  return (
    <div className="space-y-5">
      <Alert className={canSubmit ? "" : "border-destructive/40 bg-destructive/5 text-destructive"}>
        <AlertTitle>{t("tournamentPredictionDeadlineTitle")}</AlertTitle>
        <AlertDescription>{t("tournamentPredictionDeadlineDesc")}</AlertDescription>
      </Alert>

      <section className="space-y-3">
        <div className="flex items-center gap-2">
          <ListOrdered className="h-4 w-4 text-muted-foreground" />
          <h2 className="text-lg font-semibold">{t("groupStandingPredictions")}</h2>
        </div>
        {orderedGroups.length === 0 ? (
          <p className="rounded-lg border bg-white p-6 text-center text-sm text-muted-foreground">{t("noGroupsAvailable")}</p>
        ) : (
          <div className="grid gap-4 xl:grid-cols-2">
            {orderedGroups.map((group) => {
              const picks = groupDraft(group);
              const groupOpen = canSubmit && group.predictionOpen;
              return (
                <div key={group.groupName} className="rounded-lg border bg-white p-4 shadow-sm">
                  <div className="mb-3 flex items-center justify-between gap-3">
                    <h3 className="font-semibold">{t("group")} {group.groupName}</h3>
                    <Badge variant={groupOpen ? "success" : "muted"}>{groupOpen ? t("open") : t("closed")}</Badge>
                  </div>
                  <div className="grid gap-3">
                    {[0, 1, 2, 3].map((position) => (
                      <PositionSelect
                        key={position}
                        label={positionLabel(position, t)}
                        teams={group.teams}
                        value={picks[position] ?? ""}
                        selectedValues={picks}
                        disabled={!groupOpen}
                        onChange={(teamId) => updateGroupDraft(group.groupName, position, teamId)}
                      />
                    ))}
                  </div>
                  {group.predictionSubmittedAt ? (
                    <p className="mt-3 text-xs text-muted-foreground">{t("lastSubmitted")} {formatDateTime(group.predictionSubmittedAt, language)}</p>
                  ) : null}
                  <Button className="mt-4" size="sm" disabled={submitGroupMutation.isPending || !groupOpen} onClick={() => saveGroup(group)}>
                    {t("saveGroupPrediction")}
                  </Button>
                </div>
              );
            })}
          </div>
        )}
      </section>

      <section className="rounded-lg border bg-white p-4 shadow-sm">
        <div className="mb-3 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Trophy className="h-4 w-4 text-muted-foreground" />
            <h2 className="text-lg font-semibold">{t("finalRankingPrediction")}</h2>
          </div>
          <Badge variant={finalRanking && canSubmit && finalRanking.predictionOpen ? "success" : "muted"}>
            {finalRanking && canSubmit && finalRanking.predictionOpen ? t("open") : t("closed")}
          </Badge>
        </div>
        {!finalRanking ? (
          <p className="py-6 text-center text-sm text-muted-foreground">{t("loadingPool")}</p>
        ) : (
          <div className="grid gap-3 md:grid-cols-2">
            <PositionSelect
              label={t("champion")}
              teams={finalRanking.teams}
              value={finalPicks.championTeamId}
              selectedValues={Object.values(finalPicks)}
              disabled={!canSubmit || !finalRanking.predictionOpen}
              onChange={(teamId) => updateFinalDraft("championTeamId", teamId)}
            />
            <PositionSelect
              label={t("runnerUp")}
              teams={finalRanking.teams}
              value={finalPicks.runnerUpTeamId}
              selectedValues={Object.values(finalPicks)}
              disabled={!canSubmit || !finalRanking.predictionOpen}
              onChange={(teamId) => updateFinalDraft("runnerUpTeamId", teamId)}
            />
            <PositionSelect
              label={t("thirdPlace")}
              teams={finalRanking.teams}
              value={finalPicks.thirdPlaceTeamId}
              selectedValues={Object.values(finalPicks)}
              disabled={!canSubmit || !finalRanking.predictionOpen}
              onChange={(teamId) => updateFinalDraft("thirdPlaceTeamId", teamId)}
            />
            <PositionSelect
              label={t("fourthPlace")}
              teams={finalRanking.teams}
              value={finalPicks.fourthPlaceTeamId}
              selectedValues={Object.values(finalPicks)}
              disabled={!canSubmit || !finalRanking.predictionOpen}
              onChange={(teamId) => updateFinalDraft("fourthPlaceTeamId", teamId)}
            />
            <div className="md:col-span-2">
              {finalRanking.predictionSubmittedAt ? (
                <p className="mb-3 text-xs text-muted-foreground">{t("lastSubmitted")} {formatDateTime(finalRanking.predictionSubmittedAt, language)}</p>
              ) : null}
              <Button disabled={submitFinalMutation.isPending || !canSubmit || !finalRanking.predictionOpen} onClick={saveFinalRanking}>
                {t("saveFinalRanking")}
              </Button>
            </div>
          </div>
        )}
      </section>

      <section className="rounded-lg border bg-white p-4 shadow-sm">
        <div className="mb-3 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Trophy className="h-4 w-4 text-muted-foreground" />
            <h2 className="text-lg font-semibold">{t("topScorerPrediction")}</h2>
          </div>
          <Badge variant={topScorer && canSubmit && topScorer.predictionOpen ? "success" : "muted"}>
            {topScorer && canSubmit && topScorer.predictionOpen ? t("open") : t("closed")}
          </Badge>
        </div>
        {!topScorer ? (
          <p className="py-6 text-center text-sm text-muted-foreground">{t("loadingPool")}</p>
        ) : (
          <div className="grid gap-3 md:grid-cols-3">
            <label className="grid gap-1 text-sm">
              <span className="font-medium">{t("topScorerCountry")}</span>
              <select
                aria-label={t("topScorerCountry")}
                className="h-10 rounded-md border border-input bg-white px-3 text-sm shadow-sm disabled:cursor-not-allowed disabled:opacity-60"
                value={topScorerPick.teamId}
                disabled={!canSubmit || !topScorer.predictionOpen}
                onChange={(event) => updateTopScorerDraft("teamId", event.target.value)}
              >
                <option value="">{t("selectTeam")}</option>
                {topScorer.teams.map((team) => (
                  <option key={team.id} value={team.id}>
                    {flagForFifaCode(team.fifaCode)} {team.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="grid gap-1 text-sm">
              <span className="font-medium">{t("topScorerPlayer")}</span>
              <Input
                aria-label={t("topScorerPlayer")}
                value={topScorerPick.playerName}
                disabled={!canSubmit || !topScorer.predictionOpen}
                maxLength={120}
                placeholder={t("topScorerPlayerPlaceholder")}
                onChange={(event) => updateTopScorerDraft("playerName", event.target.value)}
              />
            </label>
            <label className="grid gap-1 text-sm">
              <span className="font-medium">{t("topScorerGoals")}</span>
              <select
                aria-label={t("topScorerGoals")}
                className="h-10 rounded-md border border-input bg-white px-3 text-sm shadow-sm disabled:cursor-not-allowed disabled:opacity-60"
                value={topScorerPick.goals}
                disabled={!canSubmit || !topScorer.predictionOpen}
                onChange={(event) => updateTopScorerDraft("goals", event.target.value)}
              >
                <option value="">{t("selectGoals")}</option>
                {Array.from({ length: 15 }, (_, index) => index + 1).map((goals) => (
                  <option key={goals} value={String(goals)}>
                    {goals}
                  </option>
                ))}
              </select>
            </label>
            <div className="md:col-span-3">
              {topScorer.predictionSubmittedAt ? (
                <p className="mb-3 text-xs text-muted-foreground">{t("lastSubmitted")} {formatDateTime(topScorer.predictionSubmittedAt, language)}</p>
              ) : null}
              <Button disabled={submitTopScorerMutation.isPending || !canSubmit || !topScorer.predictionOpen} onClick={saveTopScorer}>
                {t("saveTopScorer")}
              </Button>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

function PositionSelect({
  label,
  teams,
  value,
  selectedValues,
  disabled,
  onChange,
}: {
  label: string;
  teams: TeamSummary[];
  value: string;
  selectedValues: string[];
  disabled: boolean;
  onChange: (teamId: string) => void;
}) {
  const { t } = useLanguage();
  const selected = new Set(selectedValues.filter(Boolean));

  return (
    <label className="grid gap-1 text-sm">
      <span className="font-medium">{label}</span>
      <select
        aria-label={label}
        className="h-10 rounded-md border border-input bg-white px-3 text-sm shadow-sm disabled:cursor-not-allowed disabled:opacity-60"
        value={value}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
      >
        <option value="">{t("selectTeam")}</option>
        {teams.map((team) => (
          <option key={team.id} value={team.id} disabled={selected.has(team.id) && team.id !== value}>
            {flagForFifaCode(team.fifaCode)} {team.name}
          </option>
        ))}
      </select>
    </label>
  );
}

function MatchFilters({
  filters,
  groups,
  teams,
  onChange,
}: {
  filters: MatchFiltersState;
  groups: string[];
  teams: TeamSummary[];
  onChange: (filters: MatchFiltersState) => void;
}) {
  const { t } = useLanguage();
  const selectedTeamLabel =
    filters.teams.length === 0
      ? t("selectFlag")
      : teams
          .filter((team) => filters.teams.includes(team.fifaCode))
          .map((team) => `${flagForFifaCode(team.fifaCode)} ${team.fifaCode}`)
          .join(", ");

  function toggleTeam(fifaCode: string) {
    onChange({
      ...filters,
      teams: filters.teams.includes(fifaCode)
        ? filters.teams.filter((selected) => selected !== fifaCode)
        : [...filters.teams, fifaCode],
    });
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-3 pt-5 lg:flex-row lg:items-start">
        <Filter className="hidden h-4 w-4 text-muted-foreground sm:block" />
        <select
          aria-label={t("round")}
          className="h-10 rounded-md border border-input bg-white px-3 text-sm shadow-sm lg:w-44"
          value={filters.round}
          onChange={(event) => onChange({ ...filters, round: event.target.value })}
        >
          <option value="">{t("allRounds")}</option>
          {MATCH_ROUNDS.map((round) => (
            <option key={round} value={round}>
              {roundLabel(round, t)}
            </option>
          ))}
        </select>
        <select
          aria-label={t("group")}
          className="h-10 rounded-md border border-input bg-white px-3 text-sm shadow-sm lg:w-40"
          value={filters.group}
          onChange={(event) => onChange({ ...filters, group: event.target.value })}
        >
          <option value="">{t("allGroups")}</option>
          {groups.map((group) => (
            <option key={group} value={group}>
              {t("group")} {group}
            </option>
          ))}
        </select>
        <details className="relative lg:w-80">
          <summary className="flex h-10 cursor-pointer list-none items-center rounded-md border border-input bg-white px-3 text-sm shadow-sm">
            <span className="truncate">{selectedTeamLabel}</span>
          </summary>
          <div className="absolute z-20 mt-2 max-h-72 w-full overflow-y-auto rounded-md border bg-white p-2 shadow-lg">
            {teams.map((team) => (
              <label key={team.id} className="flex cursor-pointer items-center gap-2 rounded px-2 py-2 text-sm hover:bg-muted">
                <input
                  type="checkbox"
                  checked={filters.teams.includes(team.fifaCode)}
                  onChange={() => toggleTeam(team.fifaCode)}
                />
                <span>{flagForFifaCode(team.fifaCode)}</span>
                <span className="font-mono">{team.fifaCode}</span>
                <span className="truncate text-muted-foreground">{team.name}</span>
              </label>
            ))}
          </div>
        </details>
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
  const { language, t } = useLanguage();

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
                <TableHead>{t("group")}</TableHead>
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
                      {prediction.managedParticipant?.name ?? prediction.user?.username}
                      {prediction.mine ? <Badge variant="success">{t("mine")}</Badge> : null}
                    </div>
                  </TableCell>
                  <TableCell>{prediction.match.groupName ?? "-"}</TableCell>
                  <TableCell>
                    {participantFlag(prediction.match, "home")} {participantCode(prediction.match, "home")} vs{" "}
                    {participantFlag(prediction.match, "away")} {participantCode(prediction.match, "away")}
                  </TableCell>
                  <TableCell className="font-semibold">
                    {prediction.homeScore}-{prediction.awayScore}
                  </TableCell>
                  <TableCell>{formatDateTime(prediction.submittedAt, language)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}

function ManagedParticipantsCard({
  participants,
  match,
  predictions,
  form,
  createMutation,
  removeMutation,
  submitMutation,
  drafts,
  setDrafts,
  setFeedback,
}: {
  participants: ManagedParticipant[];
  match: MatchSummary | undefined;
  predictions: Map<string, PoolPrediction>;
  form: UseFormReturn<ManagedParticipantForm>;
  createMutation: UseMutationResult<ManagedParticipant, Error, ManagedParticipantForm>;
  removeMutation: UseMutationResult<void, Error, string>;
  submitMutation: UseMutationResult<PredictionResponse, Error, { participantId: string; homeScore: number; awayScore: number }>;
  drafts: ManagedScoreDraft;
  setDrafts: Dispatch<SetStateAction<ManagedScoreDraft>>;
  setFeedback: Dispatch<SetStateAction<FeedbackMessage>>;
}) {
  const { t } = useLanguage();

  function draftFor(participantId: string) {
    const existing = predictions.get(participantId);
    const draft = drafts[participantId];
    return {
      homeScore: draft?.homeScore ?? String(existing?.homeScore ?? ""),
      awayScore: draft?.awayScore ?? String(existing?.awayScore ?? ""),
    };
  }

  function updateDraft(participantId: string, key: "homeScore" | "awayScore", value: string) {
    setDrafts((current) => ({
      ...current,
      [participantId]: {
        homeScore: current[participantId]?.homeScore ?? "",
        awayScore: current[participantId]?.awayScore ?? "",
        [key]: value,
      },
    }));
  }

  function savePrediction(participantId: string) {
    if (!match?.predictionOpen) {
      setFeedback({ type: "error", message: t("predictionsClosed") });
      return;
    }
    const draft = draftFor(participantId);
    const homeScore = Number(draft.homeScore);
    const awayScore = Number(draft.awayScore);
    if (!Number.isInteger(homeScore) || !Number.isInteger(awayScore) || homeScore < 0 || awayScore < 0) {
      setFeedback({ type: "error", message: t("invalidScores") });
      return;
    }
    submitMutation.mutate({ participantId, homeScore, awayScore });
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("managedParticipants")}</CardTitle>
        <CardDescription>{t("managedParticipantsDesc")}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <form onSubmit={form.handleSubmit((values) => createMutation.mutate(values))} className="grid gap-3 sm:grid-cols-[1fr_auto] sm:items-end">
          {form.formState.errors.root ? (
            <Alert className="border-destructive/30 sm:col-span-2">
              <AlertDescription>{form.formState.errors.root.message}</AlertDescription>
            </Alert>
          ) : null}
          <FormField label={t("participantName")} error={form.formState.errors.name}>
            <Input {...form.register("name")} />
          </FormField>
          <Button disabled={createMutation.isPending}>
            <UserPlus className="h-4 w-4" />
            {t("create")}
          </Button>
        </form>

        {participants.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">{t("noManagedParticipants")}</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("participantName")}</TableHead>
                <TableHead className="min-w-56">{t("prediction")}</TableHead>
                <TableHead className="w-12"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {participants.map((participant) => {
                const draft = draftFor(participant.participantId);
                return (
                  <TableRow key={participant.participantId}>
                    <TableCell className="font-medium">{participant.name}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Input
                          aria-label={`${participant.name} home score`}
                          type="number"
                          min={0}
                          className="h-9 w-16"
                          value={draft.homeScore}
                          disabled={!match?.predictionOpen}
                          onChange={(event) => updateDraft(participant.participantId, "homeScore", event.target.value)}
                        />
                        <span className="text-muted-foreground">-</span>
                        <Input
                          aria-label={`${participant.name} away score`}
                          type="number"
                          min={0}
                          className="h-9 w-16"
                          value={draft.awayScore}
                          disabled={!match?.predictionOpen}
                          onChange={(event) => updateDraft(participant.participantId, "awayScore", event.target.value)}
                        />
                        <Button
                          size="sm"
                          disabled={!match || submitMutation.isPending}
                          onClick={() => savePrediction(participant.participantId)}
                        >
                          {t("save")}
                        </Button>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        disabled={removeMutation.isPending}
                        onClick={() => removeMutation.mutate(participant.participantId)}
                        title={t("remove")}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}

function hasResolvedParticipants(match: MatchSummary) {
  return Boolean(match.homeTeam && match.awayTeam);
}

function isTournamentPredictionOpen() {
  return new Date() < TOURNAMENT_PREDICTION_FRONTEND_DEADLINE;
}

function hasCompleteDistinctPicks(values: string[], expectedLength: number) {
  const filled = values.filter(Boolean);
  return filled.length === expectedLength && new Set(filled).size === expectedLength;
}

function finalRankingDraftValues(
  finalRanking: TournamentRankingResponse | undefined,
  draft: Partial<FinalRankingDraft>,
): TournamentRankingPicks {
  return {
    championTeamId: draft.championTeamId ?? finalRanking?.predicted?.championTeamId ?? "",
    runnerUpTeamId: draft.runnerUpTeamId ?? finalRanking?.predicted?.runnerUpTeamId ?? "",
    thirdPlaceTeamId: draft.thirdPlaceTeamId ?? finalRanking?.predicted?.thirdPlaceTeamId ?? "",
    fourthPlaceTeamId: draft.fourthPlaceTeamId ?? finalRanking?.predicted?.fourthPlaceTeamId ?? "",
  };
}

function topScorerDraftValues(topScorer: TopScorerResponse | undefined, draft: Partial<TopScorerDraft>): TopScorerDraft {
  return {
    teamId: draft.teamId ?? topScorer?.predicted?.teamId ?? "",
    playerName: draft.playerName ?? topScorer?.predicted?.playerName ?? "",
    goals: draft.goals ?? (topScorer?.predicted?.goals ? String(topScorer.predicted.goals) : ""),
  };
}

function positionLabel(position: number, t: ReturnType<typeof useLanguage>["t"]) {
  switch (position) {
    case 0:
      return t("firstPlace");
    case 1:
      return t("secondPlace");
    case 2:
      return t("thirdPlace");
    case 3:
      return t("fourthPlace");
    default:
      return `${position + 1}`;
  }
}

function FeedbackAlert({ feedback }: { feedback: Exclude<FeedbackMessage, null> }) {
  const isError = feedback.type === "error";
  return (
    <Alert className={isError ? "mt-4 border-destructive/40 bg-destructive/5 text-destructive" : "mt-4"}>
      <div className="flex items-start gap-3">
        {isError ? (
          <TriangleAlert className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
        ) : (
          <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" aria-hidden="true" />
        )}
        <AlertDescription className={isError ? "text-destructive/90" : undefined}>{feedback.message}</AlertDescription>
      </div>
    </Alert>
  );
}

function predictionErrorMessage(error: unknown, t: (key: "predictionFailed" | "predictionsClosed") => string) {
  if (error instanceof ApiError && error.status === 409 && error.message === "Predictions are closed for this match") {
    return t("predictionsClosed");
  }
  return error instanceof Error && error.message ? error.message : t("predictionFailed");
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

function uniqueGroups(matches: MatchSummary[]) {
  return [...new Set(matches.map((match) => match.groupName).filter((group): group is string => Boolean(group)))]
    .sort((a, b) => a.localeCompare(b));
}

function filterAndSortMatches(matches: MatchSummary[], filters: MatchFiltersState) {
  const selectedTeams = new Set(filters.teams);
  return matches
    .filter((match) => {
      if (filters.group && match.groupName !== filters.group) return false;
      if (filters.round && match.stage !== filters.round) return false;
      if (filters.predictableOnly && !match.predictionOpen) return false;
      if (selectedTeams.size === 0) return true;

      const homeCode = match.homeTeam?.fifaCode;
      const awayCode = match.awayTeam?.fifaCode;
      return Boolean((homeCode && selectedTeams.has(homeCode)) || (awayCode && selectedTeams.has(awayCode)));
    })
    .sort((a, b) => {
      const comparison = new Date(a.kickoffAt).getTime() - new Date(b.kickoffAt).getTime();
      return filters.sortDirection === "asc" ? comparison : -comparison;
    });
}

function roundLabel(stage: string, t: ReturnType<typeof useLanguage>["t"]) {
  switch (stage) {
    case "GROUP_STAGE":
      return t("roundGroups");
    case "ROUND_OF_32":
      return t("roundOf32");
    case "ROUND_OF_16":
      return t("roundOf16");
    case "QUARTER_FINAL":
      return t("roundOf8");
    case "SEMI_FINAL":
      return t("semifinals");
    case "THIRD_PLACE":
      return t("thirdPlace");
    case "FINAL":
      return t("final");
    default:
      return stage;
  }
}
