create table managed_participants (
    id uuid primary key,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    pool_id uuid not null references prediction_pools(id),
    display_name varchar(80) not null,
    constraint uk_managed_participant_pool_name unique (pool_id, display_name)
);

alter table predictions
    alter column user_id drop not null,
    add column managed_participant_id uuid references managed_participants(id),
    add constraint ck_prediction_participant_type check (
        (user_id is not null and managed_participant_id is null)
        or (user_id is null and managed_participant_id is not null)
    );

create unique index uk_prediction_per_managed_participant
    on predictions(pool_id, match_id, managed_participant_id)
    where managed_participant_id is not null;

alter table score_events
    alter column user_id drop not null,
    add column managed_participant_id uuid references managed_participants(id),
    add constraint ck_score_event_participant_type check (
        (user_id is not null and managed_participant_id is null)
        or (user_id is null and managed_participant_id is not null)
    );

alter table prediction_current_scores
    alter column user_id drop not null,
    add column managed_participant_id uuid references managed_participants(id),
    add constraint ck_prediction_current_score_participant_type check (
        (user_id is not null and managed_participant_id is null)
        or (user_id is null and managed_participant_id is not null)
    );

alter table leaderboard_entries
    alter column user_id drop not null,
    add column managed_participant_id uuid references managed_participants(id),
    drop constraint uk_leaderboard_pool_user,
    add constraint ck_leaderboard_participant_type check (
        (user_id is not null and managed_participant_id is null)
        or (user_id is null and managed_participant_id is not null)
    );

create unique index uk_leaderboard_pool_user
    on leaderboard_entries(pool_id, user_id)
    where user_id is not null;

create unique index uk_leaderboard_pool_managed_participant
    on leaderboard_entries(pool_id, managed_participant_id)
    where managed_participant_id is not null;

create index idx_managed_participants_pool_id on managed_participants(pool_id);
create index idx_predictions_pool_managed_participant on predictions(pool_id, managed_participant_id);
create index idx_score_events_pool_managed_participant on score_events(pool_id, managed_participant_id);
create index idx_prediction_current_scores_pool_managed_participant on prediction_current_scores(pool_id, managed_participant_id);
