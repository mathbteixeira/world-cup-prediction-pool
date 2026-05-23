# world-cup-prediction-pool

World Cup prediction pool backend built with Java 21, Spring Boot 3, PostgreSQL, Flyway, Spring Security with JWT, Docker Compose, OpenAPI, JUnit, and Testcontainers.

## Proposed architecture

Use a modular monolith with clean boundaries by business capability. Keep HTTP, application services, and persistence adapters thin, while the domain model owns invariants such as prediction deadlines, scoring auditability, and pool membership rules.

### Package layout

```text
io.github.mathbteixeira.worldcuppredictionpool
├── auth
│   ├── api
│   └── application
├── common
│   └── model
├── config
├── pool
│   ├── api
│   ├── application
│   ├── domain
│   └── persistence
├── prediction
│   └── domain
├── scoring
│   └── domain
├── tournament
│   └── domain
└── user
    ├── domain
    └── persistence
```

### Aggregates and boundaries

- **User** aggregate: identity, credentials, role, and account status.
- **PredictionPool** aggregate: pool metadata, invite code, and membership rules. `PoolMembership` belongs to this consistency boundary.
- **Tournament** aggregate: tournament metadata, participating teams, scheduled matches, and official results.
- **Prediction** aggregate: a user prediction for one match within one pool. Invariants: one prediction per user/pool/match and submission only before kickoff.
- **ScoringRuleSet** aggregate: point rules versioned per tournament or competition configuration.
- **ScoreEntry** aggregate: immutable audit ledger for awarded points, tied back to prediction, match, and rule version.

### Main domain entities

- `UserAccount`
- `PredictionPool`
- `PoolMembership`
- `Tournament`
- `Team`
- `Match`
- `MatchResult`
- `Prediction`
- `ScoringRuleSet`
- `ScoreEntry`

### Database tables

| Table | Purpose |
| --- | --- |
| `user_accounts` | Application users, credentials, and roles |
| `prediction_pools` | Pool metadata and invite codes |
| `pool_memberships` | User membership and role inside a pool |
| `tournaments` | Tournament lifecycle metadata |
| `teams` | Teams participating in a tournament |
| `matches` | Scheduled fixtures and kickoff timestamps |
| `match_results` | Official final results entered by admins |
| `predictions` | User predictions scoped by pool and match |
| `scoring_rule_sets` | Configurable scoring rules |
| `score_entries` | Immutable scoring ledger for audit/recalculation |

### API surface

#### Auth and identity
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/token`
- `GET /api/v1/auth/me`

#### Pools
- `POST /api/v1/pools`
- `GET /api/v1/pools`
- `POST /api/v1/pools/{poolId}/join`
- `GET /api/v1/pools/{poolId}/leaderboard` *(next milestone)*

#### Tournament administration
- `POST /api/v1/admin/tournaments`
- `POST /api/v1/admin/tournaments/{tournamentId}/teams`
- `POST /api/v1/admin/tournaments/{tournamentId}/matches`
- `PUT /api/v1/admin/matches/{matchId}/result`
- `PUT /api/v1/admin/tournaments/{tournamentId}/scoring-rules`

#### Predictions and scoring
- `POST /api/v1/pools/{poolId}/matches/{matchId}/predictions`
- `GET /api/v1/pools/{poolId}/matches/{matchId}/predictions/me`
- `POST /api/v1/admin/scoring/recalculate?tournamentId={id}`
- `GET /api/v1/pools/{poolId}/leaderboard`
- `GET /api/v1/pools/{poolId}/score-audit?userId={id}`

### Why this structure works

- **Auditability:** scoring is written as immutable `score_entries`, not overwritten totals.
- **Recalculation:** scores can be recomputed from predictions + official results + rule set version.
- **Security:** JWT secures user APIs, with admin-only tournament/result endpoints.
- **Extensibility:** the modular monolith can later split scoring or notifications into separate services if scale demands it.

## First implementation milestones

1. **Foundation**
   - Spring Boot skeleton, Docker Compose, PostgreSQL, Flyway, JWT, OpenAPI, baseline schema
   - user registration/login
   - pool creation/join/list
2. **Tournament admin**
   - CRUD for tournaments, teams, matches, and official results
3. **Predictions**
   - submit/update predictions before kickoff with invariant checks
4. **Scoring engine**
   - configurable scoring rules, immutable score ledger, recalculation endpoint
5. **Leaderboards and audit**
   - pool standings, tie-breakers, detailed audit trail
6. **Hardening**
   - role-based authorization, idempotency, observability, CI, and broader integration tests

## What is implemented in this repository now

This repository now contains the milestone-1 foundation:
- Spring Boot 3 / Java 21 project scaffold
- PostgreSQL + Flyway baseline schema
- JWT-based authentication and current-user endpoint
- pool create/list/join endpoints
- OpenAPI/Swagger UI configuration
- focused unit tests plus a Testcontainers-backed repository test scaffold

## Running locally

```bash
docker compose up -d postgres
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
mvn spring-boot:run
```

Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.
