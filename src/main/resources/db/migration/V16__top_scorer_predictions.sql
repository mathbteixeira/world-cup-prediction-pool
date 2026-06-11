alter table scoring_rules
    add column if not exists top_scorer_player_points integer not null default 20,
    add column if not exists top_scorer_goals_points  integer not null default 10;

create table players (
    id uuid primary key default gen_random_uuid(),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    team_id uuid not null references teams(id),
    name varchar(120) not null,
    roster_number integer not null,
    constraint uk_player_team_name unique (team_id, name)
);

create index idx_players_team_id on players(team_id);

-- The final 2026 national-team squads are not official yet. This seed creates
-- a complete editable player catalog shape (26 per team) so the local demo and
-- API flow work now; future migrations can replace these placeholders with the
-- official squad names when available.
insert into players (id, created_at, updated_at, team_id, name, roster_number)
select
    gen_random_uuid(),
    now(),
    now(),
    t.id,
    t.fifa_code || ' Player ' || lpad(gs.roster_number::text, 2, '0'),
    gs.roster_number
from teams t
cross join generate_series(1, 26) as gs(roster_number)
where not exists (
    select 1
    from players p
    where p.team_id = t.id
);

create table top_scorer_predictions (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    pool_id uuid not null references prediction_pools(id),
    user_id uuid not null references user_accounts(id),
    tournament_id uuid not null references tournaments(id),
    player_id uuid not null references players(id),
    predicted_goals integer not null,
    submitted_at timestamptz not null,
    constraint uk_top_scorer_prediction unique (pool_id, user_id),
    constraint ck_top_scorer_prediction_goals check (predicted_goals between 1 and 15)
);

create table tournament_top_scorer_results (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    tournament_id uuid not null unique references tournaments(id),
    player_id uuid not null references players(id),
    goals integer not null,
    confirmed boolean not null,
    finalized_at timestamptz not null,
    result_checksum varchar(120) not null,
    constraint ck_tournament_top_scorer_goals check (goals between 1 and 15)
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
    rule_version integer not null,
    result_checksum varchar(120) not null,
    recalculated_at timestamptz not null
);
