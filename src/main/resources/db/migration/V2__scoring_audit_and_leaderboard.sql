drop table if exists score_entries;
drop table if exists scoring_rule_sets;

alter table prediction_pools
    add column if not exists tournament_id uuid;

alter table prediction_pools
    drop constraint if exists fk_prediction_pool_tournament;

alter table prediction_pools
    add constraint fk_prediction_pool_tournament
    foreign key (tournament_id) references tournaments(id);

alter table match_results
    add column if not exists result_checksum varchar(120) not null default 'legacy';

create table if not exists scoring_rules (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tournament_id uuid not null references tournaments(id),
    exact_score_points integer not null,
    outcome_points integer not null,
    goal_difference_bonus_points integer not null,
    rule_version integer not null,
    active boolean not null,
    constraint uk_scoring_rule_tournament_version unique (tournament_id, rule_version)
);

create table if not exists score_events (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    match_id uuid not null references matches(id),
    prediction_id uuid not null references predictions(id),
    points_awarded integer not null,
    exact_score_points_awarded integer not null,
    outcome_points_awarded integer not null,
    goal_difference_bonus_points_awarded integer not null,
    explanation varchar(255) not null,
    rule_version integer not null,
    result_checksum varchar(120) not null,
    calculated_at timestamptz not null,
    constraint uk_score_event_prediction_checksum unique (prediction_id, result_checksum)
);

create table if not exists prediction_current_scores (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    prediction_id uuid not null unique references predictions(id),
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    match_id uuid not null references matches(id),
    points_awarded integer not null,
    rule_version integer not null,
    result_checksum varchar(120) not null,
    updated_at_score timestamptz not null
);

create table if not exists leaderboard_entries (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    total_points integer not null,
    rank_position integer not null,
    recalculated_at timestamptz not null,
    constraint uk_leaderboard_pool_user unique (pool_id, user_id)
);

create index if not exists idx_score_events_match on score_events(match_id);
create index if not exists idx_score_events_pool_user on score_events(pool_id, user_id);
create index if not exists idx_prediction_current_scores_pool_user on prediction_current_scores(pool_id, user_id);
create index if not exists idx_leaderboard_pool_rank on leaderboard_entries(pool_id, rank_position);
