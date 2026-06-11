-- Extend the versioned scoring rule configuration with point values for the
-- new group-position and tournament final-ranking prediction types.
-- Existing rows keep scoring behaviour unchanged; the new columns default to
-- the product rules (10 points per correct group position and 20/18/15/15 for
-- the tournament podium) so already-seeded rules remain valid.
alter table scoring_rules
    add column if not exists group_position_points integer not null default 10,
    add column if not exists champion_points        integer not null default 20,
    add column if not exists runner_up_points       integer not null default 18,
    add column if not exists third_place_points      integer not null default 15,
    add column if not exists fourth_place_points     integer not null default 15;