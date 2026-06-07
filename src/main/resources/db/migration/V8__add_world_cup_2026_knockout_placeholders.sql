WITH knockout_seed (
                    tournament_id,
                    stage,
                    home_placeholder,
                    away_placeholder,
                    kickoff_at
    ) AS (
    VALUES
        -- ROUND OF 32
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '2A', '2B', '2026-06-28T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1C', '2F', '2026-06-29T17:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1E', '3ABCDF', '2026-06-29T20:30:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1F', '2C', '2026-06-30T01:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '2E', '2I', '2026-06-30T17:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1I', '3CDFGH', '2026-06-30T21:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1A', '3CEFHI', '2026-07-01T01:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1L', '3EHIJK', '2026-07-01T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1G', '3AEHIJ', '2026-07-01T20:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1D', '3BEFIJ', '2026-07-02T00:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1H', '2J', '2026-07-02T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '2K', '2L', '2026-07-02T23:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1B', '3EFGIJ', '2026-07-03T03:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '2D', '2G', '2026-07-03T18:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1J', '2H', '2026-07-03T22:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1K', '3DEIJL', '2026-07-04T01:30:00Z'::timestamptz),

        -- ROUND OF 16
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'W73', 'W75', '2026-07-04T17:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'W74', 'W77', '2026-07-04T21:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'W76', 'W78', '2026-07-05T20:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'W79', 'W80', '2026-07-06T00:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'W83', 'W84', '2026-07-06T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'W81', 'W82', '2026-07-07T00:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'W86', 'W88', '2026-07-07T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'W85', 'W87', '2026-07-07T20:00:00Z'::timestamptz),

        -- QUARTER-FINALS
        ('11111111-1111-1111-1111-111111111111'::uuid, 'QUARTER_FINAL', 'W89', 'W90', '2026-07-09T20:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'QUARTER_FINAL', 'W93', 'W94', '2026-07-10T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'QUARTER_FINAL', 'W91', 'W92', '2026-07-11T21:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'QUARTER_FINAL', 'W95', 'W96', '2026-07-12T01:00:00Z'::timestamptz),

        -- SEMI-FINALS
        ('11111111-1111-1111-1111-111111111111'::uuid, 'SEMI_FINAL', 'W97', 'W98', '2026-07-14T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'SEMI_FINAL', 'W99', 'W100', '2026-07-15T19:00:00Z'::timestamptz),

        -- THIRD PLACE
        ('11111111-1111-1111-1111-111111111111'::uuid, 'THIRD_PLACE', 'RU101', 'RU102', '2026-07-18T21:00:00Z'::timestamptz),

        -- FINAL
        ('11111111-1111-1111-1111-111111111111'::uuid, 'FINAL', 'W101', 'W102', '2026-07-19T19:00:00Z'::timestamptz)
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
    home_placeholder,
    away_placeholder,
    kickoff_at,
    status
)
SELECT
    gen_random_uuid(),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    knockout_seed.tournament_id,
    knockout_seed.stage,
    NULL,
    NULL,
    NULL,
    knockout_seed.home_placeholder,
    knockout_seed.away_placeholder,
    knockout_seed.kickoff_at,
    'SCHEDULED'
FROM knockout_seed
WHERE NOT EXISTS (
    SELECT 1
    FROM matches existing_match
    WHERE existing_match.tournament_id = knockout_seed.tournament_id
      AND existing_match.stage = knockout_seed.stage
      AND existing_match.kickoff_at = knockout_seed.kickoff_at
      AND existing_match.home_placeholder = knockout_seed.home_placeholder
      AND existing_match.away_placeholder = knockout_seed.away_placeholder
);
