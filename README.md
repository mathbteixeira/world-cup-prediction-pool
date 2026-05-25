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
- **Prediction aggregate** (`Prediction`)
  - one prediction per user/pool/match
- **Scoring aggregate**
  - mutable rule config (`ScoringRule`)
  - immutable audit events (`ScoreEvent`)
  - mutable projections (`PredictionCurrentScore`, `LeaderboardEntry`)

### Invariants

- one prediction per `(pool, match, user)`
- prediction submission/update allowed only **before kickoff**
- score events are immutable and append-only per `(prediction, resultChecksum)`
- replaying the same result is idempotent (no duplicated points)
- leaderboard totals are rebuilt from current per-prediction scores inside one transaction

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
  Team homeTeam;
  Team awayTeam;
  Instant kickoffAt;

  boolean canAcceptPredictionsAt(Instant now) {
    return now.isBefore(kickoffAt);
  }
}
```

```java
@Entity
class Prediction {
  UUID id;
  PredictionPool pool;
  Match match;
  UserAccount user;
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
  UserAccount user;
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
  UserAccount user;
  int totalPoints;
  int rankPosition;
  Instant recalculatedAt;
}
```

---

## Scoring engine

Scoring logic is isolated from controllers/repositories in `scoring.engine`:

- `PredictionScoringEngine` (interface)
- `DefaultPredictionScoringEngine` (implementation)
- `ScoringRuleDefinition` (versioned rule contract)
- `ScoreBreakdown` (why points were awarded)

### Current rules (v1)

- exact score: **5**
- correct winner/draw: **3**
- correct goal difference bonus: **2** (when outcome is correct and exact score is not)
- wrong prediction: **0**
- no prediction: **0**

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
- `PUT /api/v1/pools/{poolId}/matches/{matchId}/prediction`
- `PUT /api/v1/admin/matches/{matchId}/result`
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

```bash
curl -sS -X POST "$BASE_URL/api/v1/pools" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Office Pool",
    "description": "MVP pool",
    "tournamentId": "11111111-1111-1111-1111-111111111111"
  }'
```

Example response:

```json
{
  "id": "22222222-2222-2222-2222-222222222222",
  "tournamentId": "11111111-1111-1111-1111-111111111111",
  "name": "Office Pool",
  "description": "MVP pool",
  "inviteCode": "ABCD1234",
  "membershipRole": "OWNER"
}
```

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
  "name": "Office Pool",
  "description": "MVP pool",
  "inviteCode": "ABCD1234",
  "membershipRole": "MEMBER"
}
```

### 5) Submit prediction

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

### 6) Admin upserts match result (triggers recalculation)

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

### 7) View leaderboard

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
- no frontend yet
- no score-audit read endpoint yet
- no async recalculation yet

---

## Database & migrations

- Flyway baseline schema (`V1`) + scoring/recalculation schema (`V2`)
- PostgreSQL-specific constraints/indexes for idempotency and ranking access paths

---

## Testing strategy

- **Unit tests**: scoring engine and deadline enforcement behavior
- **Integration tests (Testcontainers + PostgreSQL)**:
  - result upsert
  - idempotent replay
  - recalculation after result change
  - leaderboard rebuild behavior

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

---

## Tradeoffs and future improvements

### Current tradeoffs

- leaderboard rebuild is deterministic and safe, but recomputes per affected pool (simple and reliable over micro-optimized)
- idempotency is implemented at DB constraint + application orchestration level
- rule versioning supports evolution but currently ships with v1 default fallback

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
