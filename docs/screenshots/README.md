# Screenshot Guide

Screenshots are generated from the real local app. Do not create placeholder images for screens that have not been exercised against the backend.

## Start the App

From the repository root:

```bash
docker compose up -d postgres
mvn spring-boot:run
```

From a second terminal:

```bash
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`.

## Current Capture Status

No screenshot images are committed yet. In the Codex environment used for this pass, Docker was not available: Testcontainers reported that it could not find a valid Docker environment, so the backend could not be run with the local PostgreSQL dependency for honest end-to-end screenshots.

## Local Accounts

The `local` profile seeds a demo admin account by default for local manual testing:

- Email: `admin@example.com`
- Password: `admin12345`

Register a normal user in the UI for non-admin flows.

The `dev` profile requires explicit opt-in with `APP_DEMO_ADMIN_ENABLED=true`. Do not enable the demo admin seed in deployed or production environments.

## Capture Checklist

Save images in this directory with these filenames:

- `dashboard.png`: log in, then capture the dashboard.
- `pool-detail.png`: create or open a pool, then capture the pool detail page.
- `prediction-submit.png`: open a pool with a match where `predictionOpen` is true, enter a score, and capture before or after clicking **Save**.
- `leaderboard.png`: submit at least one prediction, update the official result as admin, then capture the leaderboard tab.
- `admin-result-update.png`: log in as the demo admin, open a pool, switch to **Tournament Admin**, and capture the result update panel.

If a requested screen cannot be reached, leave the image absent and document the missing flow in this file instead of staging a fake screenshot.
