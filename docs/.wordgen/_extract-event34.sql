SET NOCOUNT ON;
DECLARE @like NVARCHAR(200) = N'%Event 34 - English Presentation Day%';

DECLARE @json NVARCHAR(MAX) = (
SELECT
  (SELECT TOP 1
        e.id, e.title, d.name AS khoa_to_chuc, e.capacity, e.status,
        e.created_at AS mo_dang_ky, e.start_time AS bat_dau, e.end_time AS ket_thuc,
        (SELECT COUNT(*) FROM registration r2 WHERE r2.event_id = e.id) AS tong_dang_ky,
        (SELECT COUNT(*) FROM registration r2 WHERE r2.event_id = e.id AND r2.status = N'REGISTERED') AS so_registered,
        (SELECT COUNT(*) FROM registration r2 WHERE r2.event_id = e.id AND r2.status = N'WAITLIST') AS so_waitlist
   FROM event e JOIN department d ON d.id = e.department_id
   WHERE e.title LIKE @like
   ORDER BY e.id
   FOR JSON PATH, WITHOUT_ARRAY_WRAPPER) AS [event],

  (SELECT
        RANK() OVER (ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
                              r.priority_score DESC, r.registration_date ASC) AS hang,
        u.full_name, s.student_code, s.major AS chuyen_nganh,
        u.semester AS hoc_ky, u.total_points AS diem_hoat_dong,
        r.registration_date, r.priority_score, r.status
   FROM registration r
   JOIN student s ON s.id = r.student_id
   JOIN users u ON u.id = s.user_id
   JOIN event e ON e.id = r.event_id
   WHERE e.title LIKE @like
   ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
            r.priority_score DESC, r.registration_date ASC
   FOR JSON PATH) AS ranking,

  (SELECT
        ROW_NUMBER() OVER (ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
                                    r.priority_score DESC, r.registration_date ASC) AS thu_hang,
        u.full_name, s.student_code, s.major,
        r.registration_date, r.priority_score,
        CAST(CASE WHEN EXISTS (SELECT 1 FROM ticket t WHERE t.registration_id = r.id) THEN 1 ELSE 0 END AS BIT) AS co_ve
   FROM registration r
   JOIN student s ON s.id = r.student_id
   JOIN users u ON u.id = s.user_id
   JOIN event e ON e.id = r.event_id
   WHERE e.title LIKE @like AND r.status = N'REGISTERED'
   ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
            r.priority_score DESC, r.registration_date ASC
   FOR JSON PATH) AS registered,

  (SELECT
        ROW_NUMBER() OVER (ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
                                    r.priority_score DESC, r.registration_date ASC) AS thu_hang_cho,
        u.full_name, s.student_code, s.major,
        r.registration_date, r.priority_score, r.note
   FROM registration r
   JOIN student s ON s.id = r.student_id
   JOIN users u ON u.id = s.user_id
   JOIN event e ON e.id = r.event_id
   WHERE e.title LIKE @like AND r.status = N'WAITLIST'
   ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
            r.priority_score DESC, r.registration_date ASC
   FOR JSON PATH) AS waitlist
FOR JSON PATH, WITHOUT_ARRAY_WRAPPER);

SELECT @json AS json_out;
