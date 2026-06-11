alter table scoring_rules
    add column if not exists top_scorer_player_points integer not null default 20,
    add column if not exists top_scorer_goals_points  integer not null default 10;

create table top_scorer_predictions (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    tournament_id uuid not null references tournaments(id),
    team_id uuid not null references teams(id),
    player_name varchar(120) not null,
    predicted_goals integer not null,
    submitted_at timestamptz not null,
    constraint uk_top_scorer_prediction unique (pool_id, user_id),
    constraint ck_top_scorer_prediction_goals check (predicted_goals between 1 and 15)
);

create table top_scorer_score_events (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    tournament_id uuid not null references tournaments(id),
    prediction_id uuid not null references top_scorer_predictions(id),
    points_awarded integer not null,
    player_points_awarded integer not null,
    goals_points_awarded integer not null,
    player_correct boolean not null,
    goals_correct boolean not null,
    explanation varchar(500) not null,
    rule_version integer not null,
    result_checksum varchar(120) not null,
    calculated_at timestamptz not null,
    constraint uk_top_scorer_score_prediction_checksum unique (prediction_id, result_checksum)
);

create table top_scorer_current_scores (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    prediction_id uuid not null unique references top_scorer_predictions(id),
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    points_awarded integer not null,
    player_correct boolean not null,
    goals_correct boolean not null,
    rule_version integer not null,
    result_checksum varchar(120) not null,
    recalculated_at timestamptz not null
);
