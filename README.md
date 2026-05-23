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

## API surface (current + target)

### Implemented now

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/token`
- `GET /api/v1/auth/me`
- `POST /api/v1/pools`
- `GET /api/v1/pools`
- `POST /api/v1/pools/{poolId}/join`

### Domain-ready for next endpoints

- submit/update predictions
- admin result entry + recalculation trigger
- leaderboard and score audit endpoints

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

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
- explicit CI workflow with build/test/security gates and branch protections
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
