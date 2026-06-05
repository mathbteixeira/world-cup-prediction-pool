alter table prediction_pools
    add column pool_scope varchar(20) not null default 'TOURNAMENT',
    add column single_match_id uuid references matches(id),
    add constraint ck_prediction_pool_scope check (pool_scope in ('TOURNAMENT', 'SINGLE_MATCH')),
    add constraint ck_prediction_pool_tournament_scope check (
        (pool_scope = 'TOURNAMENT' and single_match_id is null)
        or (pool_scope = 'SINGLE_MATCH' and single_match_id is not null)
    );

create index idx_prediction_pools_single_match_id on prediction_pools(single_match_id);
