-- Group-stage final position predictions.
--
-- Users predict the 1st-to-4th ordering of the four teams in a group. Scoring
-- awards points per correctly placed team. The tables mirror the match-scoring
-- design: immutable append-only score events (idempotent per prediction +
-- confirmed-standings checksum) feed a mutable current-score projection, which
-- the shared leaderboard rebuild aggregates.

create table group_standing_predictions (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    tournament_id uuid not null references tournaments(id),
    group_name varchar(1) not null,
    first_place_team_id uuid not null references teams(id),
    second_place_team_id uuid not null references teams(id),
    third_place_team_id uuid not null references teams(id),
    fourth_place_team_id uuid not null references teams(id),
    submitted_at timestamptz not null,
    constraint uk_group_standing_prediction unique (pool_id, user_id, group_name)
);

create table group_official_standings (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tournament_id uuid not null references tournaments(id),
    group_name varchar(1) not null,
    first_place_team_id uuid not null references teams(id),
    second_place_team_id uuid not null references teams(id),
    third_place_team_id uuid not null references teams(id),
    fourth_place_team_id uuid not null references teams(id),
    confirmed boolean not null,
    finalized_at timestamptz not null,
    result_checksum varchar(120) not null,
    constraint uk_group_official_standing unique (tournament_id, group_name)
);

create table group_standing_score_events (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    tournament_id uuid not null references tournaments(id),
    prediction_id uuid not null references group_standing_predictions(id),
    group_name varchar(1) not null,
    points_awarded integer not null,
    correct_positions integer not null,
    explanation varchar(255) not null,
    rule_version integer not null,
    result_checksum varchar(120) not null,
    calculated_at timestamptz not null,
    constraint uk_group_standing_score_event unique (prediction_id, result_checksum)
);

create table group_standing_current_scores (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    prediction_id uuid not null unique references group_standing_predictions(id),
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    group_name varchar(1) not null,
    points_awarded integer not null,
    rule_version integer not null,
    result_checksum varchar(120) not null,
    updated_at_score timestamptz not null
);

create index idx_group_standing_predictions_pool_user on group_standing_predictions(pool_id, user_id);
create index idx_group_standing_predictions_tournament_group on group_standing_predictions(tournament_id, group_name);
create index idx_group_standing_score_events_pool_user on group_standing_score_events(pool_id, user_id);
create index idx_group_standing_current_scores_pool_user on group_standing_current_scores(pool_id, user_id);