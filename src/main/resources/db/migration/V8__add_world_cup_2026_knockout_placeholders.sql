WITH knockout_seed (
    tournament_id,
    stage,
    home_placeholder,
    away_placeholder,
    kickoff_at
) AS (
    VALUES
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1A', '2B', '2026-06-29T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1C', '2D', '2026-06-29T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1E', '2F', '2026-06-30T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1G', '2H', '2026-06-30T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1I', '2J', '2026-07-01T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1K', '2L', '2026-07-01T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1B', '3A/C/D', '2026-07-02T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1D', '3B/E/F', '2026-07-02T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1F', '3C/G/H', '2026-07-03T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1H', '3D/I/J', '2026-07-03T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1J', '3E/K/L', '2026-07-04T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '1L', '3F/G/I', '2026-07-04T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '2A', '2C', '2026-07-05T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '2E', '2G', '2026-07-05T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '2I', '2K', '2026-07-06T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_32', '2H', '2L', '2026-07-06T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'Winner R32 1', 'Winner R32 2', '2026-07-08T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'Winner R32 3', 'Winner R32 4', '2026-07-08T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'Winner R32 5', 'Winner R32 6', '2026-07-09T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'Winner R32 7', 'Winner R32 8', '2026-07-09T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'Winner R32 9', 'Winner R32 10', '2026-07-10T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'Winner R32 11', 'Winner R32 12', '2026-07-10T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'Winner R32 13', 'Winner R32 14', '2026-07-11T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'ROUND_OF_16', 'Winner R32 15', 'Winner R32 16', '2026-07-11T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'QUARTER_FINAL', 'Winner R16 1', 'Winner R16 2', '2026-07-13T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'QUARTER_FINAL', 'Winner R16 3', 'Winner R16 4', '2026-07-13T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'QUARTER_FINAL', 'Winner R16 5', 'Winner R16 6', '2026-07-14T16:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'QUARTER_FINAL', 'Winner R16 7', 'Winner R16 8', '2026-07-14T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'SEMI_FINAL', 'Winner QF 1', 'Winner QF 2', '2026-07-15T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'SEMI_FINAL', 'Winner QF 3', 'Winner QF 4', '2026-07-16T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'THIRD_PLACE', 'Loser SF 1', 'Loser SF 2', '2026-07-18T19:00:00Z'::timestamptz),
        ('11111111-1111-1111-1111-111111111111'::uuid, 'FINAL', 'Winner SF 1', 'Winner SF 2', '2026-07-19T19:00:00Z'::timestamptz)
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
