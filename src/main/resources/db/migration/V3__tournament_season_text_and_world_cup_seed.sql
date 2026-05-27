alter table tournaments
    alter column season_year type varchar(20) using season_year::varchar(20);

insert into tournaments (id, created_at, updated_at, name, slug, season_year, status)
values (
    '11111111-1111-1111-1111-111111111111',
    now(),
    now(),
    'World Cup',
    'world-cup',
    '2026',
    'OPEN'
)
on conflict do nothing;
