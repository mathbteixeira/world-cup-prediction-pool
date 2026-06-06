insert into tournaments (id, created_at, updated_at, name, slug, season_year, status)
select
    '22222222-2222-2222-2222-222222222222',
    now(),
    now(),
    'National Team Friendlies',
    'national-team-friendlies',
    '2026',
    'OPEN'
where not exists (
    select 1
    from tournaments
    where id = '22222222-2222-2222-2222-222222222222'
       or slug = 'national-team-friendlies'
);

insert into teams (id, tournament_id, name, fifa_code)
values
    ('22222222-2222-2222-2222-222222222201', '22222222-2222-2222-2222-222222222222', 'Brazil', 'BRA'),
    ('22222222-2222-2222-2222-222222222202', '22222222-2222-2222-2222-222222222222', 'Egypt', 'EGY')
on conflict (tournament_id, fifa_code) do nothing;

insert into matches (
    id,
    created_at,
    updated_at,
    tournament_id,
    home_team_id,
    away_team_id,
    kickoff_at,
    stage,
    status
)
select
    '22222222-2222-2222-2222-222222222211',
    now(),
    now(),
    '22222222-2222-2222-2222-222222222222',
    home.id,
    away.id,
    '2026-06-06T22:00:00Z'::timestamptz,
    'FRIENDLY',
    'SCHEDULED'
from teams home
join teams away
  on away.tournament_id = home.tournament_id
where home.tournament_id = '22222222-2222-2222-2222-222222222222'
  and home.fifa_code = 'BRA'
  and away.fifa_code = 'EGY'
  and not exists (
      select 1
      from matches
      where id = '22222222-2222-2222-2222-222222222211'
  );
