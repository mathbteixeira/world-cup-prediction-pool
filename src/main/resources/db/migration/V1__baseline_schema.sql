create table user_accounts (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    username varchar(50) not null unique,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    role varchar(20) not null,
    enabled boolean not null
);

create table prediction_pools (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    name varchar(120) not null,
    description varchar(500),
    invite_code varchar(20) not null unique,
    owner_id uuid not null references user_accounts(id)
);

create table pool_memberships (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    role varchar(20) not null,
    constraint uk_pool_membership unique (pool_id, user_id)
);

create table tournaments (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    name varchar(120) not null,
    slug varchar(80) not null unique,
    season_year integer not null,
    status varchar(20) not null
);

create table teams (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tournament_id uuid not null references tournaments(id),
    name varchar(100) not null,
    fifa_code varchar(3) not null,
    constraint uk_tournament_fifa_code unique (tournament_id, fifa_code)
);

create table matches (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tournament_id uuid not null references tournaments(id),
    home_team_id uuid not null references teams(id),
    away_team_id uuid not null references teams(id),
    kickoff_at timestamptz not null,
    stage varchar(60) not null,
    status varchar(20) not null
);

create table match_results (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    match_id uuid not null unique references matches(id),
    home_score integer not null,
    away_score integer not null,
    home_penalty_score integer,
    away_penalty_score integer,
    final_result boolean not null,
    finalized_at timestamptz not null
);

create table predictions (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    pool_id uuid not null references prediction_pools(id),
    match_id uuid not null references matches(id),
    user_id uuid not null references user_accounts(id),
    predicted_home_score integer not null,
    predicted_away_score integer not null,
    submitted_at timestamptz not null,
    constraint uk_prediction_per_user unique (pool_id, match_id, user_id)
);

create table scoring_rule_sets (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tournament_id uuid not null unique references tournaments(id),
    exact_score_points integer not null,
    outcome_points integer not null,
    goal_difference_points integer not null,
    rule_version integer not null
);

create table score_entries (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    match_id uuid not null references matches(id),
    prediction_id uuid not null references predictions(id),
    awarded_points integer not null,
    reason varchar(255) not null,
    scoring_version integer not null,
    calculated_at timestamptz not null
);

create index idx_pool_memberships_user_id on pool_memberships(user_id);
create index idx_matches_tournament_id on matches(tournament_id);
create index idx_predictions_pool_user on predictions(pool_id, user_id);
create index idx_score_entries_pool_user on score_entries(pool_id, user_id);
