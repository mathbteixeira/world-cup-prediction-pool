alter table top_scorer_predictions
    add column team_id uuid references teams(id),
    add column player_name varchar(120);

update top_scorer_predictions tsp
set team_id = p.team_id,
    player_name = p.name
from players p
where tsp.player_id = p.id;

alter table top_scorer_predictions
    alter column team_id set not null,
    alter column player_name set not null,
    drop column player_id;

alter table top_scorer_score_events
    add column player_correct boolean not null default false,
    add column goals_correct boolean not null default false;

alter table top_scorer_score_events
    alter column player_correct drop default,
    alter column goals_correct drop default;

alter table top_scorer_current_scores
    add column player_correct boolean not null default false,
    add column goals_correct boolean not null default false;

alter table top_scorer_current_scores
    alter column player_correct drop default,
    alter column goals_correct drop default;

drop table tournament_top_scorer_results;
drop table players;
