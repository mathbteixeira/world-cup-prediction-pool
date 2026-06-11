import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, RefreshCw, Shield, Trash2, UserPlus } from "lucide-react";
import { api, WORLD_CUP_2026_TOURNAMENT_ID } from "../api/client";
import type { AdminPoolSummary, AdminTopScorerPrediction, MatchSummary, PoolMember, RecalculationResponse, TeamSummary, TopScorerRecalculationResponse } from "../api/types";
import { useAuth } from "../auth/AuthProvider";
import { Alert, AlertDescription, AlertTitle } from "../components/ui/alert";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../components/ui/table";
import { useLanguage } from "../i18n/LanguageProvider";
import { flagForFifaCode } from "../lib/flags";
import { formatDateTime } from "../lib/utils";

type TeamUpdateDraft = {
  matchId: string;
  homeTeamId: string;
  awayTeamId: string;
};

type ResultDraft = {
  matchId: string;
  homeScore: string;
  awayScore: string;
  homePenaltyScore: string;
  awayPenaltyScore: string;
  finalResult: boolean;
};

export function AdminPage() {
  const { user } = useAuth();
  const { language, t } = useLanguage();
  const queryClient = useQueryClient();
  const [teamDraft, setTeamDraft] = useState<TeamUpdateDraft>({ matchId: "", homeTeamId: "", awayTeamId: "" });
  const [resultDraft, setResultDraft] = useState<ResultDraft>({
    matchId: "",
    homeScore: "0",
    awayScore: "0",
    homePenaltyScore: "",
    awayPenaltyScore: "",
    finalResult: true,
  });
  const [selectedPoolId, setSelectedPoolId] = useState("");
  const [memberEmail, setMemberEmail] = useState("");
  const [ownerEmail, setOwnerEmail] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);
  const [recalculation, setRecalculation] = useState<RecalculationResponse | null>(null);
  const [topScorerRecalculation, setTopScorerRecalculation] = useState<TopScorerRecalculationResponse | null>(null);

  const matchesQuery = useQuery({
    queryKey: ["matches", WORLD_CUP_2026_TOURNAMENT_ID, "admin"],
    queryFn: () => api.listMatches(WORLD_CUP_2026_TOURNAMENT_ID, {}),
    enabled: user?.role === "ADMIN",
  });
  const poolsQuery = useQuery({
    queryKey: ["admin-pools"],
    queryFn: api.listAdminPools,
    enabled: user?.role === "ADMIN",
  });
  const membersQuery = useQuery({
    queryKey: ["admin-pool-members", selectedPoolId],
    queryFn: () => api.listPoolMembers(selectedPoolId),
    enabled: user?.role === "ADMIN" && Boolean(selectedPoolId),
  });
  const topScorerPredictionsQuery = useQuery({
    queryKey: ["admin-top-scorer-predictions", WORLD_CUP_2026_TOURNAMENT_ID],
    queryFn: () => api.listAdminTopScorerPredictions(WORLD_CUP_2026_TOURNAMENT_ID),
    enabled: user?.role === "ADMIN",
  });

  useEffect(() => {
    if (!selectedPoolId && poolsQuery.data?.length) {
      setSelectedPoolId(poolsQuery.data[0].id);
    }
  }, [poolsQuery.data, selectedPoolId]);

  const matches = matchesQuery.data ?? [];
  const teams = useMemo(() => uniqueTeams(matches), [matches]);
  const knockoutMatches = matches.filter(isKnockoutMatch);
  const resolvedMatches = matches.filter(hasResolvedParticipants);
  const selectedPool = poolsQuery.data?.find((pool) => pool.id === selectedPoolId);

  const updateTeams = useMutation({
    mutationFn: () => api.resolveParticipants(teamDraft.matchId, { homeTeamId: teamDraft.homeTeamId, awayTeamId: teamDraft.awayTeamId }),
    onSuccess: async () => {
      setFeedback(t("adminTeamsUpdated"));
      setTeamDraft({ matchId: "", homeTeamId: "", awayTeamId: "" });
      await queryClient.invalidateQueries({ queryKey: ["matches", WORLD_CUP_2026_TOURNAMENT_ID] });
    },
    onError: (error) => setFeedback(error instanceof Error ? error.message : t("adminActionFailed")),
  });
  const updateResult = useMutation({
    mutationFn: () =>
      api.upsertResult(resultDraft.matchId, {
        homeScore: Number(resultDraft.homeScore),
        awayScore: Number(resultDraft.awayScore),
        homePenaltyScore: resultDraft.homePenaltyScore === "" ? null : Number(resultDraft.homePenaltyScore),
        awayPenaltyScore: resultDraft.awayPenaltyScore === "" ? null : Number(resultDraft.awayPenaltyScore),
        finalResult: resultDraft.finalResult,
      }),
    onSuccess: async (response) => {
      setRecalculation(response);
      setFeedback(t("adminResultUpdated"));
      await queryClient.invalidateQueries({ queryKey: ["matches", WORLD_CUP_2026_TOURNAMENT_ID] });
    },
    onError: (error) => setFeedback(error instanceof Error ? error.message : t("adminActionFailed")),
  });
  const validateTopScorer = useMutation({
    mutationFn: ({ predictionId, playerCorrect, goalsCorrect }: { predictionId: string; playerCorrect: boolean; goalsCorrect: boolean }) =>
      api.validateTopScorerPrediction(WORLD_CUP_2026_TOURNAMENT_ID, predictionId, { playerCorrect, goalsCorrect }),
    onSuccess: async (response) => {
      setTopScorerRecalculation(response);
      setFeedback(t("topScorerResultUpdated"));
      await queryClient.invalidateQueries({ queryKey: ["admin-top-scorer-predictions", WORLD_CUP_2026_TOURNAMENT_ID] });
    },
    onError: (error) => setFeedback(error instanceof Error ? error.message : t("adminActionFailed")),
  });
  const deletePool = useMutation({
    mutationFn: api.deleteAdminPool,
    onSuccess: async () => {
      setSelectedPoolId("");
      await queryClient.invalidateQueries({ queryKey: ["admin-pools"] });
    },
  });
  const addMember = useMutation({
    mutationFn: () => api.addPoolMember(selectedPoolId, { email: memberEmail }),
    onSuccess: async () => {
      setMemberEmail("");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["admin-pools"] }),
        queryClient.invalidateQueries({ queryKey: ["admin-pool-members", selectedPoolId] }),
      ]);
    },
  });
  const removeMember = useMutation({
    mutationFn: (member: PoolMember) => api.removePoolMember(selectedPoolId, member.userId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["admin-pools"] }),
        queryClient.invalidateQueries({ queryKey: ["admin-pool-members", selectedPoolId] }),
      ]);
    },
  });
  const transferOwner = useMutation({
    mutationFn: () => api.transferPoolOwnership(selectedPoolId, { email: ownerEmail }),
    onSuccess: async () => {
      setOwnerEmail("");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["admin-pools"] }),
        queryClient.invalidateQueries({ queryKey: ["admin-pool-members", selectedPoolId] }),
      ]);
    },
  });

  if (user?.role !== "ADMIN") {
    return (
      <Alert className="border-destructive/30">
        <AlertTitle>{t("adminOnly")}</AlertTitle>
        <AlertDescription>{t("adminOnlyDesc")}</AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="space-y-6">
      <section className="flex flex-col gap-2">
        <div className="flex items-center gap-2">
          <Shield className="h-5 w-5 text-amber-700" />
          <h1 className="text-2xl font-semibold">{t("adminPageTitle")}</h1>
        </div>
        <p className="text-sm text-muted-foreground">{t("adminPageDesc")}</p>
      </section>

      {feedback ? (
        <Alert>
          <CheckCircle2 className="h-4 w-4 text-emerald-700" />
          <AlertDescription>{feedback}</AlertDescription>
        </Alert>
      ) : null}

      <div className="grid gap-4 xl:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>{t("knockoutTeamUpdate")}</CardTitle>
            <CardDescription>{t("knockoutTeamUpdateDesc")}</CardDescription>
          </CardHeader>
          <CardContent>
            <form
              className="grid gap-3 lg:grid-cols-[1.6fr_1fr_1fr_auto] lg:items-end"
              onSubmit={(event) => {
                event.preventDefault();
                updateTeams.mutate();
              }}
            >
              <AdminSelect label={t("knockoutMatch")} value={teamDraft.matchId} onChange={(value) => setTeamDraft({ ...teamDraft, matchId: value })}>
                <option value="">{t("selectMatch")}</option>
                {knockoutMatches.map((match) => (
                  <option key={match.matchId} value={match.matchId}>
                    {participantCode(match, "home")} vs {participantCode(match, "away")} - {formatDateTime(match.kickoffAt, language)}
                  </option>
                ))}
              </AdminSelect>
              <AdminSelect label={t("homeTeam")} value={teamDraft.homeTeamId} onChange={(value) => setTeamDraft({ ...teamDraft, homeTeamId: value })}>
                <TeamOptions teams={teams} label={t("selectTeam")} />
              </AdminSelect>
              <AdminSelect label={t("awayTeam")} value={teamDraft.awayTeamId} onChange={(value) => setTeamDraft({ ...teamDraft, awayTeamId: value })}>
                <TeamOptions teams={teams} label={t("selectTeam")} />
              </AdminSelect>
              <Button disabled={updateTeams.isPending || !teamDraft.matchId || !teamDraft.homeTeamId || !teamDraft.awayTeamId}>
                {t("updateTeams")}
              </Button>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t("resultUpdate")}</CardTitle>
            <CardDescription>{t("resultUpdateDesc")}</CardDescription>
          </CardHeader>
          <CardContent>
            <form
              className="grid gap-3 lg:grid-cols-[1.5fr_repeat(4,0.7fr)_auto] lg:items-end"
              onSubmit={(event) => {
                event.preventDefault();
                updateResult.mutate();
              }}
            >
              <AdminSelect label={t("match")} value={resultDraft.matchId} onChange={(value) => setResultDraft({ ...resultDraft, matchId: value })}>
                <option value="">{t("selectMatch")}</option>
                {resolvedMatches.map((match) => (
                  <option key={match.matchId} value={match.matchId}>
                    {participantCode(match, "home")} vs {participantCode(match, "away")} - {formatDateTime(match.kickoffAt, language)}
                  </option>
                ))}
              </AdminSelect>
              <AdminInput label={t("home")} value={resultDraft.homeScore} onChange={(value) => setResultDraft({ ...resultDraft, homeScore: value })} />
              <AdminInput label={t("away")} value={resultDraft.awayScore} onChange={(value) => setResultDraft({ ...resultDraft, awayScore: value })} />
              <AdminInput label={t("homePens")} value={resultDraft.homePenaltyScore} onChange={(value) => setResultDraft({ ...resultDraft, homePenaltyScore: value })} />
              <AdminInput label={t("awayPens")} value={resultDraft.awayPenaltyScore} onChange={(value) => setResultDraft({ ...resultDraft, awayPenaltyScore: value })} />
              <Button disabled={updateResult.isPending || !resultDraft.matchId}>
                <RefreshCw className="h-4 w-4" />
                {t("recalculate")}
              </Button>
              <label className="flex items-center gap-2 text-sm lg:col-span-6">
                <input
                  type="checkbox"
                  checked={resultDraft.finalResult}
                  onChange={(event) => setResultDraft({ ...resultDraft, finalResult: event.target.checked })}
                />
                {t("finalResult")}
              </label>
            </form>
            {recalculation ? (
              <p className="mt-3 text-sm text-muted-foreground">
                {t("scoredPredictions")}: {recalculation.scoredPredictions} · {t("affectedPools")}: {recalculation.affectedPools}
              </p>
            ) : null}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t("topScorerResultUpdate")}</CardTitle>
            <CardDescription>{t("topScorerResultUpdateDesc")}</CardDescription>
          </CardHeader>
          <CardContent>
            <TopScorerValidationTable
              predictions={topScorerPredictionsQuery.data ?? []}
              pending={validateTopScorer.isPending}
              onValidate={(predictionId, playerCorrect, goalsCorrect) => validateTopScorer.mutate({ predictionId, playerCorrect, goalsCorrect })}
            />
            {topScorerRecalculation ? (
              <p className="mt-3 text-sm text-muted-foreground">
                {t("scoredPredictions")}: {topScorerRecalculation.scoredPredictions} · {t("affectedPools")}: {topScorerRecalculation.affectedPools}
              </p>
            ) : null}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{t("poolModeration")}</CardTitle>
          <CardDescription>{t("poolModerationDesc")}</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 xl:grid-cols-[1.3fr_1fr]">
          <AdminPoolsTable
            pools={poolsQuery.data ?? []}
            selectedPoolId={selectedPoolId}
            onSelect={setSelectedPoolId}
            onDelete={(pool) => {
              if (window.confirm(t("deletePoolConfirm"))) deletePool.mutate(pool.id);
            }}
          />
          <PoolMembersPanel
            pool={selectedPool}
            members={membersQuery.data ?? []}
            memberEmail={memberEmail}
            ownerEmail={ownerEmail}
            onMemberEmailChange={setMemberEmail}
            onOwnerEmailChange={setOwnerEmail}
            onAddMember={() => addMember.mutate()}
            onRemoveMember={(member) => removeMember.mutate(member)}
            onTransferOwner={() => transferOwner.mutate()}
            pending={addMember.isPending || removeMember.isPending || transferOwner.isPending}
          />
        </CardContent>
      </Card>
    </div>
  );
}

function AdminPoolsTable({
  pools,
  selectedPoolId,
  onSelect,
  onDelete,
}: {
  pools: AdminPoolSummary[];
  selectedPoolId: string;
  onSelect: (poolId: string) => void;
  onDelete: (pool: AdminPoolSummary) => void;
}) {
  const { t } = useLanguage();
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{t("poolName")}</TableHead>
          <TableHead>{t("owner")}</TableHead>
          <TableHead>{t("members")}</TableHead>
          <TableHead className="w-12"></TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {pools.map((pool) => (
          <TableRow key={pool.id} className={pool.id === selectedPoolId ? "bg-muted/70" : undefined}>
            <TableCell>
              <button type="button" className="text-left font-medium hover:underline" onClick={() => onSelect(pool.id)}>
                {pool.name}
              </button>
              <div className="mt-1 flex gap-1">
                <Badge variant="outline">{pool.poolScope}</Badge>
                <Badge variant="secondary">{pool.inviteCode}</Badge>
              </div>
            </TableCell>
            <TableCell>{pool.owner.username}</TableCell>
            <TableCell>{pool.memberCount}</TableCell>
            <TableCell>
              <Button type="button" variant="ghost" size="icon" title={t("deletePool")} onClick={() => onDelete(pool)}>
                <Trash2 className="h-4 w-4" />
              </Button>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

function PoolMembersPanel({
  pool,
  members,
  memberEmail,
  ownerEmail,
  onMemberEmailChange,
  onOwnerEmailChange,
  onAddMember,
  onRemoveMember,
  onTransferOwner,
  pending,
}: {
  pool: AdminPoolSummary | undefined;
  members: PoolMember[];
  memberEmail: string;
  ownerEmail: string;
  onMemberEmailChange: (value: string) => void;
  onOwnerEmailChange: (value: string) => void;
  onAddMember: () => void;
  onRemoveMember: (member: PoolMember) => void;
  onTransferOwner: () => void;
  pending: boolean;
}) {
  const { t } = useLanguage();
  if (!pool) {
    return <p className="text-sm text-muted-foreground">{t("selectPoolForModeration")}</p>;
  }

  return (
    <div className="space-y-4">
      <div>
        <h3 className="font-semibold">{pool.name}</h3>
        <p className="text-sm text-muted-foreground">{t("owner")}: {pool.owner.email}</p>
      </div>
      <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
        <Input aria-label={t("memberEmail")} placeholder={t("memberEmail")} value={memberEmail} onChange={(event) => onMemberEmailChange(event.target.value)} />
        <Button type="button" disabled={pending || !memberEmail} onClick={onAddMember}>
          <UserPlus className="h-4 w-4" />
          {t("addMember")}
        </Button>
      </div>
      <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
        <Input aria-label={t("newOwnerEmail")} placeholder={t("newOwnerEmail")} value={ownerEmail} onChange={(event) => onOwnerEmailChange(event.target.value)} />
        <Button type="button" disabled={pending || !ownerEmail} onClick={onTransferOwner}>
          {t("transferOwner")}
        </Button>
      </div>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t("user")}</TableHead>
            <TableHead>{t("email")}</TableHead>
            <TableHead>{t("role")}</TableHead>
            <TableHead className="w-12"></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {members.map((member) => (
            <TableRow key={member.userId}>
              <TableCell>{member.username}</TableCell>
              <TableCell>{member.email}</TableCell>
              <TableCell><Badge variant={member.role === "OWNER" ? "success" : "secondary"}>{member.role}</Badge></TableCell>
              <TableCell>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  disabled={pending || member.role === "OWNER"}
                  title={t("remove")}
                  onClick={() => onRemoveMember(member)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

function TopScorerValidationTable({
  predictions,
  pending,
  onValidate,
}: {
  predictions: AdminTopScorerPrediction[];
  pending: boolean;
  onValidate: (predictionId: string, playerCorrect: boolean, goalsCorrect: boolean) => void;
}) {
  const { t } = useLanguage();
  if (predictions.length === 0) {
    return <p className="text-sm text-muted-foreground">{t("noTopScorerPredictions")}</p>;
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{t("poolName")}</TableHead>
          <TableHead>{t("user")}</TableHead>
          <TableHead>{t("topScorerPrediction")}</TableHead>
          <TableHead>{t("points")}</TableHead>
          <TableHead>{t("validation")}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {predictions.map((prediction) => (
          <TableRow key={prediction.predictionId}>
            <TableCell>{prediction.poolName}</TableCell>
            <TableCell>
              <div className="font-medium">{prediction.username}</div>
              <div className="text-xs text-muted-foreground">{prediction.email}</div>
            </TableCell>
            <TableCell>
              <div className="font-medium">
                {flagForFifaCode(prediction.team.fifaCode)} {prediction.playerName}
              </div>
              <div className="text-xs text-muted-foreground">
                {prediction.team.fifaCode} · {prediction.predictedGoals} {t("topScorerGoals").toLowerCase()}
              </div>
            </TableCell>
            <TableCell>{prediction.pointsAwarded ?? "-"}</TableCell>
            <TableCell>
              <div className="flex flex-wrap gap-2">
                <Button type="button" size="sm" variant="outline" disabled={pending} onClick={() => onValidate(prediction.predictionId, true, true)}>
                  {t("validateTopScorerAndGoals")}
                </Button>
                <Button type="button" size="sm" variant="outline" disabled={pending} onClick={() => onValidate(prediction.predictionId, true, false)}>
                  {t("validateTopScorerOnly")}
                </Button>
                <Button type="button" size="sm" variant="ghost" disabled={pending} onClick={() => onValidate(prediction.predictionId, false, false)}>
                  {t("markIncorrect")}
                </Button>
                {prediction.validated ? <Badge variant="secondary">{t("validated")}</Badge> : null}
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

function AdminSelect({
  label,
  value,
  onChange,
  children,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  children: ReactNode;
}) {
  return (
    <label className="space-y-1 text-sm font-medium">
      <span>{label}</span>
      <select className="h-10 w-full rounded-md border border-input bg-white px-3 text-sm" value={value} onChange={(event) => onChange(event.target.value)}>
        {children}
      </select>
    </label>
  );
}

function AdminInput({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="space-y-1 text-sm font-medium">
      <span>{label}</span>
      <Input type="number" min={0} value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function TeamOptions({ teams, label }: { teams: TeamSummary[]; label: string }) {
  return (
    <>
      <option value="">{label}</option>
      {teams.map((team) => (
        <option key={team.id} value={team.id}>
          {flagForFifaCode(team.fifaCode)} {team.fifaCode} - {team.name}
        </option>
      ))}
    </>
  );
}

function uniqueTeams(matches: MatchSummary[]) {
  const byId = new Map<string, TeamSummary>();
  for (const match of matches) {
    if (match.homeTeam) byId.set(match.homeTeam.id, match.homeTeam);
    if (match.awayTeam) byId.set(match.awayTeam.id, match.awayTeam);
  }
  return [...byId.values()].sort((a, b) => a.fifaCode.localeCompare(b.fifaCode));
}

function hasResolvedParticipants(match: MatchSummary) {
  return Boolean(match.homeTeam && match.awayTeam);
}

function isKnockoutMatch(match: MatchSummary) {
  return match.stage !== "GROUP_STAGE";
}

function participantCode(match: MatchSummary, side: "home" | "away") {
  const team = side === "home" ? match.homeTeam : match.awayTeam;
  const placeholder = side === "home" ? match.homePlaceholder : match.awayPlaceholder;
  return team?.fifaCode ?? placeholder ?? "TBD";
}
