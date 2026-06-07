# World Cup Prediction Pool Frontend

Production-oriented React frontend for the existing Spring Boot prediction pool backend.

## Stack

- React + TypeScript + Vite
- React Router
- TanStack Query
- Tailwind CSS with shadcn/ui-style primitives
- lucide-react
- React Hook Form + Zod
- Vitest + React Testing Library

## Setup

```bash
cd frontend
npm install
npm run dev
```

The frontend runs on `http://localhost:5173`.

Set `VITE_API_BASE_URL` when the backend is not on the default:

```bash
VITE_API_BASE_URL=http://localhost:8080 npm run dev
```

## Backend

From the repository root:

```bash
docker compose up -d
./mvnw spring-boot:run
```

The backend defaults to `http://localhost:8080`. Swagger UI is available at `/swagger-ui/index.html`, and OpenAPI is available at `/v3/api-docs`.

The frontend creates pools against the seeded World Cup 2026 tournament id:

```text
11111111-1111-1111-1111-111111111111
```

## Verification Flow

1. Register a user.
2. Create a pool.
3. Open the pool detail route.
4. Filter matches.
5. Submit a prediction while `predictionOpen` is true.
6. Review visible predictions.
7. Check the leaderboard.

Admin result updates are shown only when `/api/v1/auth/me` returns role `ADMIN`. Local self-registration creates normal `USER` accounts. With the backend running under the default `local` profile, sign in as the seeded demo admin to test admin screens:

```text
Email: admin@example.com
Password: admin12345
```

Use `APP_DEMO_ADMIN_ENABLED=false` to disable that local/dev seed.

Screenshot capture instructions are in `../docs/screenshots/README.md`.

Knockout matches can now return unresolved participants with `homeTeam`/`awayTeam` as `null` and placeholder labels such as `1A` or `2B`. The frontend displays those placeholders, keeps prediction and result controls closed until the backend marks the match resolved, and exposes ADMIN participant resolution through `PUT /api/v1/admin/matches/{matchId}/participants`.

Official match results are tournament-wide, not pool-specific. ADMIN result updates live in the `Tournament Admin` tab and show the backend recalculation impact across affected pools.

Social login and persistent pool branding assets are not frontend-only features. They need backend support for OAuth identity linking and pool image/icon fields before the UI can store them honestly.
