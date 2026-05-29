alter table matches
    alter column home_team_id drop not null,
    alter column away_team_id drop not null,
    add column home_placeholder varchar(40),
    add column away_placeholder varchar(40),
    add constraint ck_match_home_participant_present check (home_team_id is not null or home_placeholder is not null),
    add constraint ck_match_away_participant_present check (away_team_id is not null or away_placeholder is not null);
