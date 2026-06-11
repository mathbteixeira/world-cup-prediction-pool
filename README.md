# World Cup Prediction Pool — Backend Engineering Showcase

A production-style backend system built with **Java 21** and **Spring Boot 3** to demonstrate senior-level backend engineering judgment: domain modeling, deterministic scoring, idempotent recalculation, auditability, and transactional consistency.

## Why this project exists

This is intentionally **not** a toy CRUD app. The core challenge is domain correctness under change:

- predictions close at kickoff
- scores are explainable and reproducible
- recalculation is safe when official results change
- leaderboard state is derived consistently from auditable events

## Tech stack

- Java 21
- Spring Boot 3
- Spring Data JPA / Hibernate
- Spring Security + JWT
- PostgreSQL
- Flyway migrations
- OpenAPI / Swagger UI
- JUnit 5 + Testcontainers
- Docker Compose
- CI-friendly test setup (Maven + Testcontainers)

---

## Domain model

### Aggregate boundaries

- **User aggregate** (`UserAccount`)
  - identity, role, account state
- **Tournament aggregate** (`Tournament`, `Team`, `Match`, `MatchResult`)
  - official competition data and result lifecycle
- **Pool aggregate** (`PredictionPool`, `PoolMembership`)
  - membership and ownership boundaries
  - pool scope: `TOURNAMENT` or `SINGLE_MATCH`
  - owner-managed participants for small single-match family/friends pools
- **Prediction aggregate** (`Prediction`)
  - one prediction per user/pool/match
- **Scoring aggregate**
  - mutable rule config (`ScoringRule`)
  - immutable audit events (`ScoreEvent`)
  - mutable projections (`PredictionCurrentScore`, `LeaderboardEntry`)

### Invariants

- one prediction per `(pool, match, user)`
- one prediction per `(pool, match, managed participant)` for owner-managed single-match pools
- managed participants have a required display name that is unique within their pool and do not have email, password, roles, or authentication
- prediction submission/update allowed only **before kickoff**
- score events are immutable and append-only per `(prediction, resultChecksum)`
- replaying the same result is idempotent (no duplicated points)
- leaderboard totals are rebuilt from current per-prediction scores inside one transaction
- a tournament pool references a tournament and no single match
- a single-match pool references exactly one match; that match may be an existing tournament match or a custom friendly created during pool setup

### Mutability design

- **Immutable by intent**: `ScoreEvent`
- **Mutable by business lifecycle**: `MatchResult`, `Prediction` (until kickoff), `ScoringRule` activation
- **Mutable projections**: `PredictionCurrentScore`, `LeaderboardEntry`

This split allows auditing and deterministic recomputation without sacrificing query efficiency.

---

## Java entity sketches (design view)

```java
@Entity
class Match {
  UUID id;
  Tournament tournament;
  Team homeTeam; // nullable until knockout participant is resolved
  Team awayTeam; // nullable until knockout participant is resolved
  String homePlaceholder; // optional, e.g. "1A"
  String awayPlaceholder; // optional, e.g. "2B"
  Instant kickoffAt;
  String stage;
  String groupName; // optional single-letter group, such as "A"

  boolean canAcceptPredictionsAt(Instant now) {
    return homeTeam != null && awayTeam != null && now.isBefore(kickoffAt);
  }
}
```

```java
@Entity
class Prediction {
  UUID id;
  PredictionPool pool;
  Match match;
  UserAccount user; // nullable when submitted for a managed participant
  ManagedParticipant managedParticipant; // nullable for registered-user predictions
  int predictedHomeScore;
  int predictedAwayScore;
  Instant submittedAt;

  void resubmit(int home, int away, Instant submittedAt) { ... }
}
```

```java
@Entity
class ScoreEvent {
  UUID id;
  Prediction prediction;
  Match match;
  PredictionPool pool;
  UserAccount user; // nullable for managed participant predictions
  ManagedParticipant managedParticipant; // nullable for registered-user predictions
  int pointsAwarded;
  int exactScorePointsAwarded;
  int outcomePointsAwarded;
  int goalDifferenceBonusPointsAwarded;
  int ruleVersion;
  String resultChecksum;
  Instant calculatedAt;
}
```

```java
@Entity
class LeaderboardEntry {
  UUID id;
  PredictionPool pool;
  UserAccount user; // nullable for managed participant rows
  ManagedParticipant managedParticipant; // nullable for registered-user rows
  int totalPoints;
  int rankPosition;
  Instant recalculatedAt;
}
```

```java
@Entity
class PredictionPool {
  UUID id;
  PoolScope poolScope; // TOURNAMENT or SINGLE_MATCH
  Tournament tournament; // retained for compatibility and scoring rule lookup
  Match singleMatch; // required only for SINGLE_MATCH pools
}
```

Owner-managed participants are currently supported only for `SINGLE_MATCH` pools. Tournament and multi-match managed-participant support is intentionally out of scope.

---

## Scoring engine

Scoring logic is isolated from controllers/repositories in `scoring.engine`:

- `PredictionScoringEngine` (interface)
- `DefaultPredictionScoringEngine` (implementation)
- `ScoringRuleDefinition` (versioned rule contract)
- `ScoreBreakdown` (why points were awarded)

### Current rules (v1)

Match predictions:

- exact score: **7**
- correct winner/draw: **3**
- correct goal difference bonus: **2** (when outcome is correct and exact score is not)
- wrong prediction: **0**
- no prediction: **0**

Tournament-pool extras:

- group-stage final standings: **10** points per team in the exact predicted group position
- final tournament ranking: champion **20**, runner-up **18**, third place **15**, fourth place **15**
- tournament top scorer: correct player **20**, plus **10** extra points if the predicted goal total also matches; predicting only the goal total without the correct player awards **0**

The current player seed creates 26 placeholder players per seeded national team so the top-scorer flow can be tested before official 2026 squads are announced. Replace or extend the player seed with official squad names when they are available.

### Example contract

```java
ScoreBreakdown score(PredictionScoreInput prediction,
                     MatchScoreInput actualResult,
                     ScoringRuleDefinition rule)
```

The output contains total and per-rule components for transparency.

---

## Recalculation flow (result upsert)

Implemented in `MatchResultScoringService` with a transactional boundary:

1. Upsert official `MatchResult`
2. Build deterministic `resultChecksum`
3. Resolve active scoring rule version
4. Score all predictions of that match
5. Persist immutable `ScoreEvent` records (unique by prediction + checksum)
6. Upsert `PredictionCurrentScore` projection
7. Rebuild `LeaderboardEntry` totals/ranking for affected pools

### Idempotency strategy

- Unique constraint on `(prediction_id, result_checksum)` in `score_events`
- Reprocessing the same result does not create duplicate score events
- Leaderboard rebuild derives from current projection table, avoiding accumulation bugs

### Auditability strategy

- Every distinct result snapshot produces its own immutable score events
- You can trace exactly which rule version and result checksum generated each score

---

## API surface (MVP implemented)

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/token`
- `GET /api/v1/auth/me`
- `POST /api/v1/pools`
- `GET /api/v1/pools`
- `POST /api/v1/pools/{poolId}/join`
- `POST /api/v1/pools/{poolId}/managed-participants`
- `GET /api/v1/pools/{poolId}/managed-participants`
- `DELETE /api/v1/pools/{poolId}/managed-participants/{participantId}`
- `GET /api/v1/tournaments`
- `GET /api/v1/tournaments/{tournamentId}/matches`
- `GET /api/v1/tournaments/{tournamentId}/teams/{teamId}/players`
- `PUT /api/v1/pools/{poolId}/matches/{matchId}/prediction`
- `PUT /api/v1/pools/{poolId}/managed-participants/{participantId}/prediction`
- `GET /api/v1/pools/{poolId}/predictions`
- `GET /api/v1/pools/{poolId}/groups`
- `PUT /api/v1/pools/{poolId}/groups/{groupName}/prediction`
- `GET /api/v1/pools/{poolId}/final-ranking`
- `PUT /api/v1/pools/{poolId}/final-ranking/prediction`
- `GET /api/v1/pools/{poolId}/top-scorer`
- `PUT /api/v1/pools/{poolId}/top-scorer/prediction`
- `PUT /api/v1/admin/matches/{matchId}/participants`
- `PUT /api/v1/admin/matches/{matchId}/result`
- `PUT /api/v1/admin/tournaments/{tournamentId}/groups/{groupName}/standings`
- `PUT /api/v1/admin/tournaments/{tournamentId}/final-ranking`
- `PUT /api/v1/admin/tournaments/{tournamentId}/top-scorer`
- `GET /api/v1/pools/{poolId}/leaderboard`

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## MVP happy path

Set base URL:

```bash
BASE_URL=http://localhost:8080
```

### 1) Register user

```bash
curl -sS -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "email": "alice@example.com",
    "password": "StrongPass123!"
  }'
```

Example response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "alice",
  "email": "alice@example.com",
  "role": "USER"
}
```

### 2) Get token

```bash
curl -sS -X POST "$BASE_URL/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "StrongPass123!"
  }'
```

Example response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "alice",
  "email": "alice@example.com",
  "role": "USER"
}
```

Use the token:

```bash
TOKEN="<jwt-token>"
```

### 3) Create pool

Tournament pools use the existing behavior. `mode` is optional and defaults to `TOURNAMENT` for backward-compatible clients.

```bash
curl -sS -X POST "$BASE_URL/api/v1/pools" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Office Pool",
    "description": "MVP pool",
    "mode": "TOURNAMENT",
    "tournamentId": "11111111-1111-1111-1111-111111111111"
  }'
```

Example response:

```json
{
  "id": "22222222-2222-2222-2222-222222222222",
  "tournamentId": "11111111-1111-1111-1111-111111111111",
  "singleMatchId": null,
  "poolScope": "TOURNAMENT",
  "name": "Office Pool",
  "description": "MVP pool",
  "inviteCode": "ABCD1234",
  "membershipRole": "OWNER"
}
```

Single-match pools can point at an existing match. The frontend first lists tournaments with `GET /api/v1/tournaments`, then loads matches only for the selected tournament.

```bash
curl -sS -X POST "$BASE_URL/api/v1/pools" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Family Friendly",
    "description": "One match, one leaderboard",
    "mode": "SINGLE_MATCH",
    "matchId": "33333333-3333-3333-3333-333333333333"
  }'
```

Or they can create a simple custom match in the same request:

```bash
curl -sS -X POST "$BASE_URL/api/v1/pools" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Family Friendly",
    "description": "One match, one leaderboard",
    "mode": "SINGLE_MATCH",
    "customMatch": {
      "homeTeam": "Brazil",
      "awayTeam": "Egypt",
      "kickoffAt": "2026-06-20T19:00:00Z",
      "competitionLabel": "Friendly"
    }
  }'
```

The custom-match path creates a backing tournament, two team records, and one scheduled match. The returned `singleMatchId` is the match id to use when submitting predictions or upserting the result.

Seed data includes a `National Team Friendlies` tournament with Brazil vs Egypt as a scheduled friendly at `2026-06-06T22:00:00Z`.

### 4) Join pool

```bash
curl -sS -X POST "$BASE_URL/api/v1/pools/22222222-2222-2222-2222-222222222222/join" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "inviteCode": "ABCD1234"
  }'
```

Example response:

```json
{
  "id": "22222222-2222-2222-2222-222222222222",
  "tournamentId": "11111111-1111-1111-1111-111111111111",
  "singleMatchId": null,
  "poolScope": "TOURNAMENT",
  "name": "Office Pool",
  "description": "MVP pool",
  "inviteCode": "ABCD1234",
  "membershipRole": "MEMBER"
}
```

### 5) Submit prediction

Discover match IDs:

```bash
curl -sS "$BASE_URL/api/v1/tournaments/11111111-1111-1111-1111-111111111111/matches?group=A&team=MEX&predictableOnly=true" \
  -H "Authorization: Bearer $TOKEN"
```

Available filters: `status`, `stage`, `group`, `team` (home or away FIFA code), `from`, `to`, and `predictableOnly`.

For a single-match pool, use the `singleMatchId` from the create-pool response in the prediction URL. The service rejects other match ids, even if they belong to the same backing tournament.

Example response:

```json
[
  {
    "matchId": "33333333-3333-3333-3333-333333333333",
    "tournamentId": "11111111-1111-1111-1111-111111111111",
    "homeTeam": {
      "id": "77777777-7777-7777-7777-777777777777",
      "name": "Mexico",
      "fifaCode": "MEX"
    },
    "awayTeam": {
      "id": "88888888-8888-8888-8888-888888888888",
      "name": "South Africa",
      "fifaCode": "RSA"
    },
    "homePlaceholder": null,
    "awayPlaceholder": null,
    "kickoffAt": "2026-06-11T16:00:00Z",
    "stage": "GROUP_STAGE",
    "groupName": "A",
    "status": "SCHEDULED",
    "result": null,
    "predictionOpen": true
  }
]
```

```bash
curl -sS -X PUT "$BASE_URL/api/v1/pools/22222222-2222-2222-2222-222222222222/matches/33333333-3333-3333-3333-333333333333/prediction" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "homeScore": 2,
    "awayScore": 1
  }'
```

Example response:

```json
{
  "predictionId": "44444444-4444-4444-4444-444444444444",
  "poolId": "22222222-2222-2222-2222-222222222222",
  "matchId": "33333333-3333-3333-3333-333333333333",
  "homeScore": 2,
  "awayScore": 1,
  "submittedAt": "2026-06-01T10:00:00Z"
}
```

### 6) View visible pool predictions

```bash
curl -sS "$BASE_URL/api/v1/pools/22222222-2222-2222-2222-222222222222/predictions" \
  -H "Authorization: Bearer $TOKEN"
```

Example response:

```json
[
  {
    "predictionId": "44444444-4444-4444-4444-444444444444",
    "poolId": "22222222-2222-2222-2222-222222222222",
    "user": {
      "userId": "55555555-5555-5555-5555-555555555555",
      "username": "alice"
    },
    "mine": true,
    "match": {
      "matchId": "33333333-3333-3333-3333-333333333333",
      "tournamentId": "11111111-1111-1111-1111-111111111111",
      "homeTeam": {
        "id": "77777777-7777-7777-7777-777777777777",
        "name": "Mexico",
        "fifaCode": "MEX"
      },
      "awayTeam": {
        "id": "88888888-8888-8888-8888-888888888888",
        "name": "South Africa",
        "fifaCode": "RSA"
      },
      "homePlaceholder": null,
      "awayPlaceholder": null,
      "kickoffAt": "2026-06-11T16:00:00Z",
      "stage": "GROUP_STAGE",
      "groupName": "A",
      "status": "SCHEDULED",
      "result": null,
      "predictionOpen": true
    },
    "homeScore": 2,
    "awayScore": 1,
    "submittedAt": "2026-06-01T10:00:00Z"
  }
]
```

The endpoint always includes the authenticated user's own predictions. Other pool members' predictions are included only after that match is closed for predictions, so users cannot copy each other's picks before kickoff.

### 7) Admin upserts match result (triggers recalculation)

```bash
curl -sS -X PUT "$BASE_URL/api/v1/admin/matches/33333333-3333-3333-3333-333333333333/result" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "homeScore": 2,
    "awayScore": 1,
    "homePenaltyScore": null,
    "awayPenaltyScore": null,
    "finalResult": true
  }'
```

Example response:

```json
{
  "matchId": "33333333-3333-3333-3333-333333333333",
  "homeScore": 2,
  "awayScore": 1,
  "homePenaltyScore": null,
  "awayPenaltyScore": null,
  "finalResult": true,
  "resultChecksum": "ab12cd34...",
  "scoredPredictions": 5,
  "affectedPools": 1,
  "idempotentReplay": false
}
```

### 8) View leaderboard

```bash
curl -sS "$BASE_URL/api/v1/pools/22222222-2222-2222-2222-222222222222/leaderboard" \
  -H "Authorization: Bearer $TOKEN"
```

Example response:

```json
[
  {
    "poolId": "22222222-2222-2222-2222-222222222222",
    "userId": "55555555-5555-5555-5555-555555555555",
    "username": "alice",
    "totalPoints": 9,
    "rankPosition": 1,
    "recalculatedAt": "2026-06-10T21:00:00Z"
  },
  {
    "poolId": "22222222-2222-2222-2222-222222222222",
    "userId": "66666666-6666-6666-6666-666666666666",
    "username": "bob",
    "totalPoints": 4,
    "rankPosition": 2,
    "recalculatedAt": "2026-06-10T21:00:00Z"
  }
]
```

### Current limitations (MVP)

- admin/tournament/match seed data still needs a proper management flow
- the frontend supports tournament pools, single-match pools backed by an existing match, and custom single-match pools
- no score-audit read endpoint yet
- no async recalculation yet

### Knockout match placeholders

Knockout matches may be known before their participants are confirmed. These matches should be stored with nullable `homeTeam`/`awayTeam` and display placeholders such as `1A`, `2B`, or `3C/D/F/G/H`.

Placeholders are intentionally stored on the match instead of creating fake rows in `teams`, keeping the team catalog limited to real national teams. A match is not open for predictions until both real team references are resolved.

Admins can resolve placeholder participants once teams qualify. Already resolved matches are not overwritten by this endpoint, avoiding accidental changes after predictions may exist:

```bash
curl -sS -X PUT "$BASE_URL/api/v1/admin/matches/33333333-3333-3333-3333-333333333333/participants" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "homeTeamId": "77777777-7777-7777-7777-777777777777",
    "awayTeamId": "88888888-8888-8888-8888-888888888888"
  }'
```

---

## Database & migrations

- Flyway baseline schema (`V1`) + scoring/recalculation schema (`V2`)
- single-match pool scope migration (`V9`) adds `pool_scope` and `single_match_id`
- national-team friendly seed migration (`V10`) adds the `National Team Friendlies` tournament and Brazil vs Egypt
- PostgreSQL-specific constraints/indexes for idempotency and ranking access paths

---

## Testing strategy

- **Unit tests**: scoring engine and deadline enforcement behavior
- **Integration tests (Testcontainers + PostgreSQL)**:
  - result upsert
  - idempotent replay
  - recalculation after result change
  - leaderboard rebuild behavior
  - single-match pool creation and scoring reuse

Run tests:

```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
mvn test
```

---

## Local run

```bash
docker compose up -d postgres
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
mvn spring-boot:run
```

Run the frontend in another terminal:

```bash
cd frontend
npm install
npm run dev
```

The app starts at `http://localhost:5173` and lets you create either a tournament pool or a single-match pool. For an existing single match, choose **Single-match pool**, then **Existing match**, select a tournament, and then select a match. For a custom friendly, choose **Custom match** and enter the teams, kickoff time, and optional competition label.

The `local` profile seeds a demo admin account by default for manual testing on a developer machine:

```text
Email: admin@example.com
Password: admin12345
```

Override or disable the local seed with `APP_DEMO_ADMIN_EMAIL`, `APP_DEMO_ADMIN_USERNAME`, `APP_DEMO_ADMIN_PASSWORD`, and `APP_DEMO_ADMIN_ENABLED=false`. The `dev` profile does not enable this seed by default; it requires `APP_DEMO_ADMIN_ENABLED=true`. The seed component is profile-gated to `local`/`dev` and must not be enabled for deployed or production environments.

Screenshot capture instructions live in [docs/screenshots/README.md](docs/screenshots/README.md). The screenshot files are intentionally absent until captured from the real running app.

---

## Tradeoffs and future improvements

### Current tradeoffs

- leaderboard rebuild is deterministic and safe, but recomputes per affected pool (simple and reliable over micro-optimized)
- idempotency is implemented at DB constraint + application orchestration level
- rule versioning supports evolution but currently ships with v1 default fallback
- custom single-match pools create a small backing tournament so existing match, prediction, scoring, and result-update code can be reused without broad schema changes
- Pagination is intentionally omitted from the match listing endpoint for the MVP because a World Cup tournament has a small, bounded number of matches. The API keeps the client flow simple while leaving pagination easy to add if the domain expands to larger competitions or historical datasets.

### Planned improvements

- admin APIs for tournament/match/rule lifecycle
- asynchronous recalculation option for very large pools
- richer audit query APIs (per user/per match/per checksum)
- observability hardening: metrics, tracing, structured audit logs

---

## Portfolio signal

This project demonstrates backend competencies expected in international Senior Backend Engineer roles:

- domain-driven aggregate thinking
- consistency over convenience
- idempotent event recording
- deterministic recalculation under mutable external truth (official results)
- practical transaction design with PostgreSQL + Spring
- test strategy that exercises real infrastructure, not just mocks
