-- inserting WC 2026 group stage matches
WITH match_seed (
                 tournament_id,
                 stage,
                 group_name,
                 home_fifa_code,
                 away_fifa_code,
                 kickoff_at
    ) AS (
    VALUES
        -- MATCHDAY 1
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'A', 'MEX', 'RSA', '2026-06-11T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'A', 'KOR', 'CZE', '2026-06-11T19:00:00Z'::timestamptz),

        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'B', 'CAN', 'BIH', '2026-06-12T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'D', 'USA', 'PAR', '2026-06-12T19:00:00Z'::timestamptz),

        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'B', 'QAT', 'SUI', '2026-06-13T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'C', 'BRA', 'MAR', '2026-06-13T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'C', 'HAI', 'SCO', '2026-06-13T22:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'D', 'AUS', 'TUR', '2026-06-14T01:00:00Z'::timestamptz),

        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'E', 'GER', 'CUW', '2026-06-14T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'F', 'NED', 'JPN', '2026-06-14T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'E', 'CIV', 'ECU', '2026-06-14T22:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'F', 'SWE', 'TUN', '2026-06-15T01:00:00Z'::timestamptz),

        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'G', 'BEL', 'EGY', '2026-06-15T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'H', 'ESP', 'CPV', '2026-06-15T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'G', 'IRN', 'NZL', '2026-06-15T22:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'H', 'KSA', 'URU', '2026-06-16T01:00:00Z'::timestamptz),

        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'I', 'FRA', 'SEN', '2026-06-16T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'I', 'IRQ', 'NOR', '2026-06-16T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'J', 'ARG', 'ALG', '2026-06-16T22:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'J', 'AUT', 'JOR', '2026-06-17T01:00:00Z'::timestamptz),

        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'K', 'POR', 'COD', '2026-06-17T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'L', 'ENG', 'CRO', '2026-06-17T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'L', 'GHA', 'PAN', '2026-06-17T22:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'K', 'UZB', 'COL', '2026-06-18T01:00:00Z'::timestamptz),

        -- MATCHDAY 2
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'A', 'CZE', 'RSA', '2026-06-18T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'B', 'SUI', 'BIH', '2026-06-18T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'B', 'CAN', 'QAT', '2026-06-18T22:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'A', 'MEX', 'KOR', '2026-06-19T01:00:00Z'::timestamptz),

        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'D', 'USA', 'AUS', '2026-06-19T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'C', 'SCO', 'MAR', '2026-06-19T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'D', 'TUR', 'PAR', '2026-06-19T22:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'C', 'BRA', 'HAI', '2026-06-20T01:00:00Z'::timestamptz),

        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'F', 'NED', 'SWE', '2026-06-20T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'E', 'GER', 'CIV', '2026-06-20T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'E', 'ECU', 'CUW', '2026-06-20T22:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'F', 'TUN', 'JPN', '2026-06-21T01:00:00Z'::timestamptz),

        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'H', 'ESP', 'KSA', '2026-06-21T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'G', 'BEL', 'IRN', '2026-06-21T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'H', 'URU', 'CPV', '2026-06-21T22:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'G', 'NZL', 'EGY', '2026-06-22T01:00:00Z'::timestamptz),

        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'J', 'ARG', 'AUT', '2026-06-22T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'I', 'FRA', 'IRQ', '2026-06-22T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'I', 'NOR', 'SEN', '2026-06-22T22:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'J', 'JOR', 'ALG', '2026-06-23T01:00:00Z'::timestamptz),

        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'K', 'POR', 'UZB', '2026-06-23T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'L', 'ENG', 'GHA', '2026-06-23T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'L', 'PAN', 'CRO', '2026-06-23T22:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'K', 'COL', 'COD', '2026-06-24T01:00:00Z'::timestamptz),

        -- MATCHDAY 3
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'B', 'SUI', 'CAN', '2026-06-24T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'B', 'BIH', 'QAT', '2026-06-24T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'C', 'SCO', 'BRA', '2026-06-24T20:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'C', 'MAR', 'HAI', '2026-06-24T20:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'A', 'CZE', 'MEX', '2026-06-25T01:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'A', 'RSA', 'KOR', '2026-06-25T01:00:00Z'::timestamptz),

        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'D', 'TUR', 'USA', '2026-06-25T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'D', 'PAR', 'AUS', '2026-06-25T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'E', 'CUW', 'CIV', '2026-06-25T20:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'E', 'ECU', 'GER', '2026-06-25T20:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'F', 'TUN', 'NED', '2026-06-26T01:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'F', 'JPN', 'SWE', '2026-06-26T01:00:00Z'::timestamptz),

        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'I', 'NOR', 'FRA', '2026-06-26T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'I', 'SEN', 'IRQ', '2026-06-26T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'H', 'CPV', 'KSA', '2026-06-26T20:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'H', 'URU', 'ESP', '2026-06-26T20:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'G', 'NZL', 'BEL', '2026-06-27T01:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'G', 'EGY', 'IRN', '2026-06-27T01:00:00Z'::timestamptz),

        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'J', 'JOR', 'ARG', '2026-06-27T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'J', 'ALG', 'AUT', '2026-06-27T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'K', 'POR', 'COL', '2026-06-27T20:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'K', 'COD', 'UZB', '2026-06-27T20:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'L', 'ENG', 'PAN', '2026-06-28T01:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'GROUP_STAGE', 'L', 'CRO', 'GHA', '2026-06-28T01:00:00Z'::timestamptz)
)
INSERT INTO matches (
    id,
    created_at,
    updated_at,
    tournament_id,
    stage,
    group_name,
    home_team_id,
    away_team_id,
    kickoff_at,
    status
)
SELECT
    gen_random_uuid(),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    match_seed.tournament_id,
    match_seed.stage,
    match_seed.group_name,
    home_team.id,
    away_team.id,
    match_seed.kickoff_at,
    'SCHEDULED'
FROM match_seed
         JOIN teams home_team
              ON home_team.tournament_id = match_seed.tournament_id
                  AND home_team.fifa_code = match_seed.home_fifa_code
         JOIN teams away_team
              ON away_team.tournament_id = match_seed.tournament_id
                  AND away_team.fifa_code = match_seed.away_fifa_code
WHERE NOT EXISTS (
    SELECT 1
    FROM matches existing_match
    WHERE existing_match.tournament_id = match_seed.tournament_id
      AND existing_match.home_team_id = home_team.id
      AND existing_match.away_team_id = away_team.id
      AND existing_match.kickoff_at = match_seed.kickoff_at
);