alter table tournaments
    alter column season_year type varchar(20) using season_year::varchar(20);

insert into tournaments (id, created_at, updated_at, name, slug, season_year, status)
select
    '11111111-1111-1111-1111-111111111111', -- default World Cup id used in API examples and bootstrap pool creation
    now(),
    now(),
    'World Cup',
    'world-cup',
    '2026',
    'OPEN'
where not exists (
    select 1
    from tournaments
    where id = '11111111-1111-1111-1111-111111111111'
       or slug = 'world-cup'
);
