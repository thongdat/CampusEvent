USE [event_management_db];
GO

SET NOCOUNT ON;

DECLARE @as_of_end DATETIME2(0) = '2026-05-19T00:00:00';
DECLARE @target_registration_rate FLOAT = 0.34;
DECLARE @target_checkin_rate FLOAT = 0.58;

DECLARE @admin_role_id BIGINT = (SELECT TOP 1 id FROM dbo.role WHERE name = 'ADMIN');
DECLARE @committee_role_id BIGINT = (SELECT TOP 1 id FROM dbo.role WHERE name = 'COMMITTEE');

IF @admin_role_id IS NOT NULL AND @committee_role_id IS NOT NULL
BEGIN
    DECLARE @kept_admin_id BIGINT = (
        SELECT TOP 1 id
        FROM dbo.users
        WHERE role_id = @admin_role_id
        ORDER BY CASE WHEN email = 'admin@example.com' THEN 0 ELSE 1 END, id
    );

    UPDATE dbo.users
    SET role_id = @committee_role_id
    WHERE role_id = @admin_role_id
      AND id <> @kept_admin_id;
END;

;WITH registered_by_event AS (
    SELECT
        e.id,
        COUNT(r.id) AS registered_count
    FROM dbo.event e
    LEFT JOIN dbo.registration r
        ON r.event_id = e.id
       AND r.status = 'REGISTERED'
    WHERE e.start_time < @as_of_end
    GROUP BY e.id
)
UPDATE e
SET capacity =
    CASE
        WHEN registered_by_event.registered_count > 0 THEN
            CASE
                WHEN CONVERT(INT, CEILING(registered_by_event.registered_count / @target_registration_rate)) < registered_by_event.registered_count + 5
                    THEN registered_by_event.registered_count + 5
                ELSE CONVERT(INT, CEILING(registered_by_event.registered_count / @target_registration_rate))
            END
        WHEN COALESCE(e.capacity, 0) > 20 THEN 20
        WHEN COALESCE(e.capacity, 0) <= 0 THEN 20
        ELSE e.capacity
    END
FROM dbo.event e
JOIN registered_by_event ON registered_by_event.id = e.id
WHERE e.start_time < @as_of_end;

DECLARE @registered INT = (
    SELECT COUNT(*)
    FROM dbo.registration r
    JOIN dbo.event e ON e.id = r.event_id
    WHERE e.start_time < @as_of_end
      AND r.status = 'REGISTERED'
);

DECLARE @attended INT = (
    SELECT COUNT(*)
    FROM dbo.attendance a
    JOIN dbo.registration r ON r.id = a.registration_id
    JOIN dbo.event e ON e.id = r.event_id
    WHERE e.start_time < @as_of_end
      AND a.status = 'ATTENDED'
);

DECLARE @target_attended INT = CONVERT(INT, CEILING(@registered * @target_checkin_rate));
DECLARE @need INT = CASE WHEN @target_attended > @attended THEN @target_attended - @attended ELSE 0 END;

INSERT INTO dbo.attendance (checkin_time, status, registration_id)
SELECT TOP (@need)
    DATEADD(MINUTE, 10 + (r.id % 35), e.start_time),
    'ATTENDED',
    r.id
FROM dbo.registration r
JOIN dbo.event e ON e.id = r.event_id
WHERE e.start_time < @as_of_end
  AND r.status = 'REGISTERED'
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.attendance a
      WHERE a.registration_id = r.id
  )
ORDER BY e.start_time, r.id;

UPDATE f
SET rating = CASE WHEN f.id % 5 IN (0, 1) THEN 5 ELSE 4 END
FROM dbo.feedback f
JOIN dbo.event e ON e.id = f.event_id
WHERE e.start_time < @as_of_end
  AND f.rating IS NOT NULL;

SELECT
    (SELECT SUM(COALESCE(capacity, 0)) FROM dbo.event WHERE start_time < @as_of_end) AS elapsed_capacity,
    (SELECT COUNT(*) FROM dbo.registration r JOIN dbo.event e ON e.id = r.event_id WHERE e.start_time < @as_of_end AND r.status = 'REGISTERED') AS elapsed_registered,
    (SELECT COUNT(*) FROM dbo.attendance a JOIN dbo.registration r ON r.id = a.registration_id JOIN dbo.event e ON e.id = r.event_id WHERE e.start_time < @as_of_end AND a.status = 'ATTENDED') AS elapsed_attended,
    (SELECT AVG(CAST(f.rating AS FLOAT)) FROM dbo.feedback f JOIN dbo.event e ON e.id = f.event_id WHERE e.start_time < @as_of_end) AS elapsed_avg_rating;
GO
