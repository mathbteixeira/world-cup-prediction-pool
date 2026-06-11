-- Tournament final-ranking (podium) predictions.
--
-- Users predict the four teams finishing 1st (champion), 2nd (runner-up), 3rd
-- and 4th. Scoring awards position-specific points (champion > runner-up >
-- third/fourth). Mirrors the match-scoring design: immutable append-only score
-- events (idempotent per prediction + confirmed-ranking checksum) feed a
-- mutable current-score projection aggregated by the shared leaderboard rebuild.

create table tournament_ranking_predictions (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    tournament_id uuid not null references tournaments(id),
    champion_team_id uuid not null references teams(id),
    runner_up_team_id uuid not null references teams(id),
    third_place_team_id uuid not null references teams(id),
    fourth_place_team_id uuid not null references teams(id),
    submitted_at timestamptz not null,
    constraint uk_tournament_ranking_prediction unique (pool_id, user_id)
);

create table tournament_official_rankings (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tournament_id uuid not null unique references tournaments(id),
    champion_team_id uuid not null references teams(id),
    runner_up_team_id uuid not null references teams(id),
    third_place_team_id uuid not null references teams(id),
    fourth_place_team_id uuid not null references teams(id),
    confirmed boolean not null,
    finalized_at timestamptz not null,
    result_checksum varchar(120) not null
);

create table tournament_ranking_score_events (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    tournament_id uuid not null references tournaments(id),
    prediction_id uuid not null references tournament_ranking_predictions(id),
    points_awarded integer not null,
    champion_points_awarded integer not null,
    runner_up_points_awarded integer not null,
    third_place_points_awarded integer not null,
    fourth_place_points_awarded integer not null,
    explanation varchar(255) not null,
    rule_version integer not null,
    result_checksum varchar(120) not null,
    calculated_at timestamptz not null,
    constraint uk_tournament_ranking_score_event unique (prediction_id, result_checksum)
);

create table tournament_ranking_current_scores (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    prediction_id uuid not null unique references tournament_ranking_predictions(id),
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    points_awarded integer not null,
    rule_version integer not null,
    result_checksum varchar(120) not null,
    updated_at_score timestamptz not null
);

create index idx_tournament_ranking_predictions_tournament on tournament_ranking_predictions(tournament_id);
create index idx_tournament_ranking_score_events_pool_user on tournament_ranking_score_events(pool_id, user_id);
create index idx_tournament_ranking_current_scores_pool_user on tournament_ranking_current_scores(pool_id, user_id);