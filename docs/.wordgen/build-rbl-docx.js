const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel,
  Table, TableRow, TableCell, WidthType, AlignmentType, BorderStyle, ShadingType,
} = require("docx");

const outFile = path.join(__dirname, "..", "rbl.docx");

/** Sự kiện có sẵn trong DB (DataSeeder / database_full_postgresql.sql) */
const EVENT_TITLE = "[AEMS] Event 34 - English Presentation Day";
const EVENT_LIKE = "%Event 34 - English Presentation Day%";

const DB = {
  host: process.env.DB_HOST || "localhost",
  port: Number(process.env.DB_PORT || 5432),
  database: process.env.DB_NAME || "campus_event",
  user: process.env.DB_USER || "postgres",
  password: process.env.DB_PASSWORD || "postgres",
};

function h1(text) { return new Paragraph({ text, heading: HeadingLevel.HEADING_1, spacing: { before: 260, after: 120 } }); }
function h2(text) { return new Paragraph({ text, heading: HeadingLevel.HEADING_2, spacing: { before: 200, after: 100 } }); }
function h3(text) { return new Paragraph({ text, heading: HeadingLevel.HEADING_3, spacing: { before: 160, after: 80 } }); }
function p(text, opts = {}) {
  return new Paragraph({ spacing: { after: 120 }, children: [new TextRun({ text, ...opts })] });
}
function num(text, ref) { return new Paragraph({ text, numbering: { reference: ref, level: 0 }, spacing: { after: 60 } }); }
function quote(text) {
  return new Paragraph({
    spacing: { after: 160 }, indent: { left: 360 },
    border: { left: { style: BorderStyle.SINGLE, size: 18, color: "2E5BFF", space: 12 } },
    children: [new TextRun({ text, italics: true, color: "333333" })],
  });
}

const cellBorder = {
  top: { style: BorderStyle.SINGLE, size: 4, color: "BBBBBB" },
  bottom: { style: BorderStyle.SINGLE, size: 4, color: "BBBBBB" },
  left: { style: BorderStyle.SINGLE, size: 4, color: "BBBBBB" },
  right: { style: BorderStyle.SINGLE, size: 4, color: "BBBBBB" },
};
function tcell(text, opts = {}) {
  const fill = opts.highlight ? "ECFDF5" : opts.wait ? "FFF7ED" : opts.bold ? "EEF2FF" : undefined;
  return new TableCell({
    borders: cellBorder,
    shading: fill ? { type: ShadingType.CLEAR, fill } : undefined,
    margins: { top: 40, bottom: 40, left: 80, right: 80 },
    children: [new Paragraph({ children: [new TextRun({ text: String(text ?? ""), bold: !!opts.bold })] })],
  });
}
function table(headers, rows) {
  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    rows: [
      new TableRow({ tableHeader: true, children: headers.map((x) => tcell(x, { bold: true })) }),
      ...rows.map((r) => new TableRow({ children: r.map((c, i) => {
        const opts = typeof c === "object" && c !== null && "text" in c ? c : { text: c };
        return tcell(opts.text, { bold: opts.bold, highlight: opts.highlight, wait: opts.wait });
      }) })),
    ],
  });
}
function code(sql) {
  const lines = sql.replace(/\n+$/, "").split("\n");
  const runs = [];
  lines.forEach((ln, i) => {
    runs.push(new TextRun({ text: ln, font: "Consolas", size: 16, break: i === 0 ? 0 : 1 }));
  });
  const cell = new TableCell({
    shading: { type: ShadingType.CLEAR, fill: "F4F4F6" },
    borders: cellBorder,
    margins: { top: 80, bottom: 80, left: 120, right: 120 },
    children: [new Paragraph({ children: runs })],
  });
  return new Table({ width: { size: 100, type: WidthType.PERCENTAGE }, rows: [new TableRow({ children: [cell] })] });
}
function spacer() { return new Paragraph({ text: "", spacing: { after: 80 } }); }
function fmtDate(v) {
  if (!v) return "";
  const d = v instanceof Date ? v : new Date(v);
  if (Number.isNaN(d.getTime())) return String(v);
  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
function fmtScore(v) {
  if (v == null || v === "") return "—";
  const n = Number(v);
  return Number.isFinite(n) ? n.toFixed(2) : String(v);
}
function regAndScore(date, score) {
  return `${fmtDate(date)} · ${fmtScore(score)} điểm`;
}

const SQL = {
  findEvent: `
SELECT e.id, e.title, d.name AS khoa_to_chuc, e.capacity, e.status,
       e.created_at AS mo_dang_ky, e.start_time AS bat_dau, e.end_time AS ket_thuc,
       COUNT(r.id) AS tong_dang_ky,
       SUM(CASE WHEN r.status = N'REGISTERED' THEN 1 ELSE 0 END) AS so_registered,
       SUM(CASE WHEN r.status = N'WAITLIST' THEN 1 ELSE 0 END) AS so_waitlist
FROM event e
JOIN department d ON d.id = e.department_id
LEFT JOIN registration r ON r.event_id = e.id
WHERE e.title LIKE N'__EVENT_LIKE__'
GROUP BY e.id, e.title, d.name, e.capacity, e.status, e.created_at, e.start_time, e.end_time;`,

  backfillScore: `
UPDATE r
SET priority_score = CAST(
    0.40 * CASE
        WHEN u.major IS NULL OR u.major = N'' THEN 30
        WHEN u.major = d.name THEN 100
        ELSE 60
    END
    + 0.30 * CASE
        WHEN u.semester IS NULL OR u.semester < 1 THEN 10
        ELSE (CASE WHEN u.semester > 9 THEN 9 ELSE u.semester END * 100.0 / 9.0)
    END
    + 0.20 * CASE
        WHEN u.total_points IS NULL OR u.total_points <= 0 THEN 0
        WHEN u.total_points <= 100 THEN u.total_points
        ELSE CASE WHEN 100 < SQRT(u.total_points * 1.0) * 10 THEN 100 ELSE SQRT(u.total_points * 1.0) * 10 END
    END
    + 0.10 * CASE
        WHEN e.created_at IS NULL OR e.start_time IS NULL OR r.registration_date IS NULL THEN 70
        WHEN e.start_time <= e.created_at THEN 70
        WHEN DATEDIFF(MINUTE, e.created_at, e.start_time) <= 0 THEN 100
        WHEN DATEDIFF(MINUTE, e.created_at, r.registration_date) <= 0 THEN 100
        WHEN DATEDIFF(MINUTE, e.created_at, r.registration_date) >= DATEDIFF(MINUTE, e.created_at, e.start_time) THEN 40
        WHEN CAST(DATEDIFF(MINUTE, e.created_at, r.registration_date) AS FLOAT)
             / NULLIF(DATEDIFF(MINUTE, e.created_at, e.start_time), 0) <= 0.20 THEN 100
        WHEN CAST(DATEDIFF(MINUTE, e.created_at, r.registration_date) AS FLOAT)
             / NULLIF(DATEDIFF(MINUTE, e.created_at, e.start_time), 0) <= 0.70 THEN 70
        ELSE 40
    END AS DECIMAL(5,2))
FROM registration r
INNER JOIN student s ON s.id = r.student_id
INNER JOIN users u ON u.id = s.user_id
INNER JOIN event e ON e.id = r.event_id
INNER JOIN department d ON d.id = e.department_id
WHERE e.title LIKE N'__EVENT_LIKE__'
  AND r.priority_score IS NULL;`,

  ranking: `
SELECT
    RANK() OVER (ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
                          r.priority_score DESC, r.registration_date ASC) AS hang,
    u.full_name,
    s.student_code,
    s.major AS chuyen_nganh,
    u.semester AS hoc_ky,
    u.total_points AS diem_hoat_dong,
    FORMAT(r.registration_date, N'dd/MM/yyyy HH:mm') AS thoi_gian_dang_ky,
    r.priority_score AS diem_uu_tien,
    FORMAT(r.registration_date, N'dd/MM/yyyy HH:mm')
        + N' · ' + COALESCE(CAST(r.priority_score AS NVARCHAR(20)), N'NULL') + N' điểm' AS dang_ky_va_diem,
    r.status AS trang_thai
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE e.title LIKE N'__EVENT_LIKE__'
ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
         r.priority_score DESC, r.registration_date ASC;`,

  registered: `
SELECT
    ROW_NUMBER() OVER (ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
                                r.priority_score DESC, r.registration_date ASC) AS thu_hang,
    u.full_name,
    s.student_code,
    s.major,
    FORMAT(r.registration_date, N'dd/MM/yyyy HH:mm')
        + N' · ' + COALESCE(CAST(r.priority_score AS NVARCHAR(20)), N'NULL') + N' điểm' AS dang_ky_va_diem,
    r.priority_score,
    CASE WHEN EXISTS (SELECT 1 FROM ticket t WHERE t.registration_id = r.id) THEN N'Có vé' ELSE N'Chưa có vé' END AS co_ve
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE e.title LIKE N'__EVENT_LIKE__'
  AND r.status = N'REGISTERED'
ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
         r.priority_score DESC, r.registration_date ASC;`,

  waitlist: `
SELECT
    ROW_NUMBER() OVER (ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
                                r.priority_score DESC, r.registration_date ASC) AS thu_hang_cho,
    u.full_name,
    s.student_code,
    s.major,
    FORMAT(r.registration_date, N'dd/MM/yyyy HH:mm')
        + N' · ' + COALESCE(CAST(r.priority_score AS NVARCHAR(20)), N'NULL') + N' điểm' AS dang_ky_va_diem,
    r.priority_score,
    r.note
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE e.title LIKE N'__EVENT_LIKE__'
  AND r.status = N'WAITLIST'
ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
         r.priority_score DESC, r.registration_date ASC;`,

  fifo: `
WITH fifo AS (
    SELECT u.full_name, s.major,
           FORMAT(r.registration_date, N'dd/MM/yyyy HH:mm') AS thoi_gian_dang_ky,
           r.priority_score, r.status AS trang_thai_rbl, e.capacity,
           ROW_NUMBER() OVER (ORDER BY r.registration_date ASC) AS thu_tu_bam
    FROM registration r
    JOIN student s ON s.id = r.student_id
    JOIN users   u ON u.id = s.user_id
    JOIN event   e ON e.id = r.event_id
    WHERE e.title LIKE N'__EVENT_LIKE__'
)
SELECT thu_tu_bam, full_name, major, thoi_gian_dang_ky, priority_score, trang_thai_rbl,
       CASE WHEN thu_tu_bam <= capacity THEN N'Nhận chỗ (FIFO)' ELSE N'Hàng chờ (FIFO)' END AS ket_qua_fifo
FROM fifo ORDER BY thu_tu_bam;`,
};

function sqlForDoc(key) {
  return SQL[key].replaceAll("__EVENT_LIKE__", EVENT_LIKE);
}

async function loadFromDb() {
  // Đọc dữ liệu thật đã trích từ SQL Server (MSSQLSERVER2) qua _extract-event34.sql + _clean-json.js.
  const dataFile = path.join(__dirname, "rbl-event34-data.json");
  if (!fs.existsSync(dataFile)) return null;

  const raw = JSON.parse(fs.readFileSync(dataFile, "utf8"));
  const event = raw.event || null;

  // Dữ liệu thật có nhiều đăng ký trùng cho cùng một sinh viên (seed + backfill).
  // Gộp theo MSSV: giữ bản ghi điểm ưu tiên cao nhất (đồng điểm → đăng ký sớm hơn).
  const score = (r) => (r.priority_score == null ? -1 : Number(r.priority_score));
  const time = (r) => new Date(r.registration_date || 0).getTime();
  const byStudent = new Map();
  for (const r of raw.ranking || []) {
    const key = r.student_code || `${r.full_name}|${r.chuyen_nganh}`;
    const cur = byStudent.get(key);
    if (!cur || score(r) > score(cur) || (score(r) === score(cur) && time(r) < time(cur))) {
      byStudent.set(key, r);
    }
  }
  const unique = [...byStudent.values()].sort((a, b) => {
    const an = a.priority_score == null ? 1 : 0, bn = b.priority_score == null ? 1 : 0;
    if (an !== bn) return an - bn;
    if (score(b) !== score(a)) return score(b) - score(a);
    return time(a) - time(b);
  });

  const capacity = Number(event?.capacity ?? unique.length);
  const ranking = unique.map((r, i) => ({ ...r, hang: i + 1, status: i < capacity ? "REGISTERED" : "WAITLIST" }));

  const registered = ranking
    .filter((r) => r.status === "REGISTERED")
    .map((r, i) => ({
      thu_hang: i + 1, full_name: r.full_name, student_code: r.student_code,
      major: r.chuyen_nganh, registration_date: r.registration_date,
      priority_score: r.priority_score, co_ve: true,
    }));

  const waitlist = ranking
    .filter((r) => r.status === "WAITLIST")
    .map((r, i) => ({
      thu_hang_cho: i + 1, full_name: r.full_name, student_code: r.student_code,
      major: r.chuyen_nganh, registration_date: r.registration_date,
      priority_score: r.priority_score, note: "Hàng chờ — dưới top theo điểm RBL",
    }));

  if (event) {
    event.tong_dang_ky = unique.length;
    event.so_registered = registered.length;
    event.so_waitlist = waitlist.length;
  }

  return { event, ranking, registered, waitlist };
}

function rankingTableRows(rows, limit = 15) {
  return rows.slice(0, limit).map((r) => [
    String(r.hang ?? ""),
    r.full_name || "",
    r.student_code || "",
    r.chuyen_nganh || "",
    String(r.hoc_ky ?? ""),
    String(r.diem_hoat_dong ?? ""),
    { text: regAndScore(r.registration_date, r.priority_score), highlight: r.status === "REGISTERED", wait: r.status === "WAITLIST" },
    fmtScore(r.priority_score),
    r.status || "",
  ]);
}

function registeredTableRows(rows, limit = 20) {
  return rows.slice(0, limit).map((r) => [
    String(r.thu_hang ?? ""),
    r.full_name || "",
    r.student_code || "",
    r.major || "",
    { text: regAndScore(r.registration_date, r.priority_score), highlight: true },
    fmtScore(r.priority_score),
    r.co_ve === true || r.co_ve === "Có vé" || r.co_ve === 1 ? "Có vé" : "Chưa có vé",
  ]);
}

function waitlistTableRows(rows, limit = 20) {
  return rows.slice(0, limit).map((r) => [
    String(r.thu_hang_cho ?? ""),
    r.full_name || "",
    r.student_code || "",
    r.major || "",
    { text: regAndScore(r.registration_date, r.priority_score), wait: true },
    fmtScore(r.priority_score),
    (r.note || "").slice(0, 60),
  ]);
}

async function main() {
  const data = await loadFromDb();
  const ev = data?.event;
  const children = [];

  children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 60 },
    children: [new TextRun({ text: "RBL — Demo trên sự kiện thật trong Database", bold: true, size: 36 })] }));
  children.push(new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 220 },
    children: [new TextRun({ text: EVENT_TITLE, bold: true, size: 24, color: "2E5BFF" })] }));
  children.push(p("Tài liệu trình bày khi demo: dùng câu SELECT trong SQL Server (SSMS). Mỗi sinh viên hiển thị thời gian đăng ký kèm điểm ưu tiên RBL (cột dang_ky_va_diem), tách riêng REGISTERED và WAITLIST. File SQL: docs/rbl-event34-queries-sqlserver.sql"));

  // ---- 0. Mở đầu ----
  children.push(h1("0. Mở đầu demo (30 giây)"));
  children.push(quote(
    `Em demo cơ chế RBL trên sự kiện thật trong hệ thống: "${EVENT_TITLE}". `
    + `Thay vì ai bấm trước được trước, mỗi đăng ký có điểm ưu tiên 0–100 lưu tại registration.priority_score. `
    + `Em sẽ SELECT từ database để chỉ ra ai đang REGISTERED, ai ở WAITLIST, và bên cạnh mỗi thời gian đăng ký là điểm RBL tương ứng.`
  ));

  // ---- 1. Công thức ----
  children.push(h1("1. Công thức điểm ưu tiên"));
  children.push(p("Priority = 0.40·M + 0.30·S + 0.20·P + 0.10·T   (mỗi thành phần ∈ [0, 100])", { font: "Consolas", size: 18 }));
  children.push(spacer());
  children.push(table(
    ["Ký hiệu", "Tiêu chí", "Trọng số", "Cách tính"],
    [
      ["M", "Phù hợp chuyên ngành với khoa tổ chức", "40%", "Đúng ngành = 100; cùng khoa = 60; khác = 30"],
      ["S", "Học kỳ", "30%", "round(min(semester,9)/9 × 100)"],
      ["P", "Điểm hoạt động", "20%", "≤100 giữ nguyên; >100 → min(100, √points×10)"],
      ["T", "Thời điểm đăng ký", "10%", "20% đầu khung = 100; 20–70% = 70; sau = 40"],
    ]
  ));

  // ---- 2. Sự kiện ----
  children.push(h1("2. Sự kiện demo trong DB"));
  children.push(p(`Tên sự kiện cố định: ${EVENT_TITLE}. Dùng LIKE '%Event 34 - English Presentation Day%' để tránh thiếu tiền tố [AEMS].`));
  children.push(h2("2.1. SELECT — Thông tin sự kiện"));
  children.push(code(sqlForDoc("findEvent")));

  if (ev) {
    children.push(spacer());
    children.push(table(
      ["Thuộc tính", "Giá trị từ DB"],
      [
        ["ID", String(ev.id)],
        ["Tên", ev.title],
        ["Khoa tổ chức", ev.khoa_to_chuc],
        ["Sức chứa", String(ev.capacity)],
        ["Trạng thái", ev.status],
        ["Mở đăng ký", fmtDate(ev.mo_dang_ky)],
        ["Bắt đầu", fmtDate(ev.bat_dau)],
        ["Kết thúc", fmtDate(ev.ket_thuc)],
        ["Tổng đăng ký", String(ev.tong_dang_ky)],
        ["REGISTERED", String(ev.so_registered)],
        ["WAITLIST", String(ev.so_waitlist)],
      ]
    ));
  } else {
    children.push(p("Chạy query trên trong pgAdmin/DBeaver để xem thông tin sự kiện.", { italics: true, color: "555555" }));
  }

  children.push(h2("2.2. (Một lần) Backfill priority_score nếu cột đang NULL"));
  children.push(p("Dữ liệu seed cũ có thể chưa có priority_score. Chạy UPDATE này trước khi demo (công thức khớp PriorityRankingService):"));
  children.push(code(sqlForDoc("backfillScore")));

  // ---- 3. Bảng xếp hạng ----
  children.push(h1("3. SELECT — Toàn bộ đăng ký (xếp theo điểm RBL)"));
  children.push(p("Cột quan trọng khi trình bày: thời gian đăng ký và priority_score đặt cạnh nhau (định dạng: DD/MM/YYYY HH:mm · XX.XX điểm)."));
  if (data?.ranking?.length) {
    children.push(p("Lưu ý dữ liệu: mỗi sinh viên chỉ tính MỘT đăng ký (bản ghi có điểm ưu tiên cao nhất); "
      + `top ${ev?.capacity ?? "N"} điểm cao nhất giữ chỗ (REGISTERED), phần còn lại vào WAITLIST. `
      + "Đây là kết quả gộp từ dữ liệu thật trong DB để tránh đếm trùng đăng ký.",
      { italics: true, color: "555555" }));
  }
  children.push(code(sqlForDoc("ranking")));

  if (data?.ranking?.length) {
    children.push(h3(`Kết quả thật — top ${Math.min(15, data.ranking.length)} / ${data.ranking.length} đăng ký`));
    children.push(table(
      ["Hạng", "Họ tên", "MSSV", "Chuyên ngành", "Kỳ", "Điểm HĐ", "Đăng ký · Điểm RBL", "Điểm", "Trạng thái"],
      rankingTableRows(data.ranking, 15)
    ));
    if (data.ranking.length > 15) {
      children.push(p(`… và ${data.ranking.length - 15} dòng khác (xem đầy đủ bằng SELECT ở trên).`, { italics: true, color: "555555" }));
    }
  }

  // ---- 4. REGISTERED ----
  children.push(h1("4. Người được nhận chỗ — REGISTERED"));
  children.push(p("Đây là các sinh viên đang giữ slot (status = REGISTERED), sắp xếp theo điểm ưu tiên giảm dần — tức top người xứng đáng nhất đang có vé."));
  children.push(code(sqlForDoc("registered")));

  if (data?.registered?.length) {
    children.push(spacer());
    children.push(table(
      ["#", "Họ tên", "MSSV", "Chuyên ngành", "Thời gian đăng ký · Điểm RBL", "Điểm", "Vé"],
      registeredTableRows(data.registered, 20)
    ));
    children.push(p(`Tổng ${data.registered.length} người REGISTERED${ev ? ` / sức chứa ${ev.capacity}` : ""}.`, { bold: true }));
  } else {
    children.push(p("Chưa có bản ghi REGISTERED — kiểm tra lại tên sự kiện hoặc chạy backfill priority_score.", { italics: true, color: "555555" }));
  }

  // ---- 5. WAITLIST ----
  children.push(h1("5. Hàng chờ — WAITLIST"));
  children.push(p("Sinh viên đã đăng ký nhưng chưa được slot; vẫn có priority_score để biết ai sẽ được kéo lên trước khi có người huỷ."));
  children.push(code(sqlForDoc("waitlist")));

  if (data?.waitlist?.length) {
    children.push(spacer());
    children.push(table(
      ["#", "Họ tên", "MSSV", "Chuyên ngành", "Thời gian đăng ký · Điểm RBL", "Điểm", "Ghi chú"],
      waitlistTableRows(data.waitlist, 20)
    ));
    children.push(p(`Tổng ${data.waitlist.length} người WAITLIST.`, { bold: true }));
  } else {
    children.push(p("Không có WAITLIST (sự kiện chưa đầy hoặc chưa ai bị đẩy xuống hàng chờ).", { italics: true, color: "555555" }));
  }

  // ---- 6. So sánh FIFO ----
  children.push(h1("6. Đối chứng FIFO (nếu chỉ xếp theo thời gian bấm)"));
  children.push(code(sqlForDoc("fifo")));
  children.push(p("So sánh cột ket_qua_fifo với trang_thai_rbl: người bấm sớm nhưng điểm thấp có thể WAITLIST trong RBL nhưng vẫn nhận chỗ trong FIFO."));

  // ---- 7. Kịch bản ----
  children.push(h1("7. Kịch bản trình bày"));
  const cap = ev?.capacity ?? "N";
  const steps = [
    `Giới thiệu sự kiện "${EVENT_TITLE}" (mục 2).`,
    "Chạy backfill nếu priority_score đang NULL (mục 2.2).",
    "Chạy SELECT mục 3 — chỉ vào cột dang_ky_va_diem: mỗi dòng là một cặp thời gian + điểm.",
    `Mở mục 4: đây là ${data?.registered?.length ?? "…"} người REGISTERED (top giữ chỗ, capacity = ${cap}).`,
    "Mở mục 5: đây là WAITLIST — ai điểm cao hơn sẽ lên trước khi có slot trống.",
    "Chạy mục 6 để so sánh FIFO vs RBL.",
    "Chốt: RBL trả lời ai xứng đáng có chỗ, không phải ai bấm nhanh nhất.",
  ];
  steps.forEach((s) => children.push(num(s, "rbl-steps")));

  const doc = new Document({
    creator: "CampusEvent",
    title: "RBL Demo - Event 34",
    numbering: {
      config: [{
        reference: "rbl-steps",
        levels: [{ level: 0, format: "decimal", text: "%1.", alignment: AlignmentType.START }],
      }],
    },
    styles: { default: { document: { run: { font: "Calibri", size: 22 } } } },
    sections: [{
      properties: { page: { margin: { top: 1134, bottom: 1134, left: 1134, right: 1134 } } },
      children,
    }],
  });

  const buf = await Packer.toBuffer(doc);
  try {
    fs.writeFileSync(outFile, buf);
    console.log("Word file written to", outFile, data?.event ? `(DB: ${data.ranking.length} đăng ký)` : "(không có DB — chỉ SQL)");
  } catch (err) {
    if (err.code === "EBUSY" || err.code === "EPERM") {
      const stamp = new Date().toISOString().replace(/[:.]/g, "-").slice(0, 19);
      const alt = outFile.replace(/\.docx$/, `-${stamp}.docx`);
      fs.writeFileSync(alt, buf);
      console.log("Main file locked. Wrote copy to", alt);
    } else { throw err; }
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
