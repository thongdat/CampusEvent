const fs = require("fs");
const path = require("path");
const {
  Document,
  Packer,
  Paragraph,
  TextRun,
  HeadingLevel,
  ImageRun,
  Table,
  TableRow,
  TableCell,
  WidthType,
  AlignmentType,
  BorderStyle,
} = require("docx");

const imagesDir = path.join(__dirname, "images");
const outFile = path.join(__dirname, "..", "CampusEvent-Workflow.docx");

// Usable content width on A4 with ~1 inch margins ≈ 600 px.
const MAX_W = 600;
const MAX_H = 760;

function pngSize(buf) {
  // IHDR: width at bytes 16-19, height at 20-23 (big-endian).
  return { width: buf.readUInt32BE(16), height: buf.readUInt32BE(20) };
}

function imageParagraph(fileName) {
  const file = path.join(imagesDir, fileName);
  const data = fs.readFileSync(file);
  const { width, height } = pngSize(data);
  let w = width;
  let h = height;
  const ratio = Math.min(MAX_W / w, MAX_H / h, 1);
  w = Math.round(w * ratio);
  h = Math.round(h * ratio);
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { before: 120, after: 240 },
    children: [
      new ImageRun({
        type: "png",
        data,
        transformation: { width: w, height: h },
      }),
    ],
  });
}

function h1(text) {
  return new Paragraph({ text, heading: HeadingLevel.HEADING_1, spacing: { before: 240, after: 120 } });
}
function h2(text) {
  return new Paragraph({ text, heading: HeadingLevel.HEADING_2, spacing: { before: 200, after: 100 } });
}
function p(runs, opts = {}) {
  const children = Array.isArray(runs)
    ? runs
    : [new TextRun({ text: runs, ...opts })];
  return new Paragraph({ children, spacing: { after: 120 } });
}
function bullet(text) {
  return new Paragraph({ text, bullet: { level: 0 }, spacing: { after: 60 } });
}
function note(text) {
  return new Paragraph({
    spacing: { after: 160 },
    children: [new TextRun({ text: "Ghi chú: " + text, italics: true, color: "555555" })],
  });
}

const cellBorder = {
  top: { style: BorderStyle.SINGLE, size: 4, color: "BBBBBB" },
  bottom: { style: BorderStyle.SINGLE, size: 4, color: "BBBBBB" },
  left: { style: BorderStyle.SINGLE, size: 4, color: "BBBBBB" },
  right: { style: BorderStyle.SINGLE, size: 4, color: "BBBBBB" },
};

function tcell(text, opts = {}) {
  return new TableCell({
    borders: cellBorder,
    margins: { top: 40, bottom: 40, left: 80, right: 80 },
    children: [
      new Paragraph({
        children: [new TextRun({ text, bold: !!opts.bold })],
      }),
    ],
  });
}

function buildTable(headers, rows) {
  const headRow = new TableRow({
    tableHeader: true,
    children: headers.map((hh) => tcell(hh, { bold: true })),
  });
  const bodyRows = rows.map(
    (r) => new TableRow({ children: r.map((c) => tcell(c)) })
  );
  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    rows: [headRow, ...bodyRows],
  });
}

const children = [];

// Title
children.push(
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 80 },
    children: [
      new TextRun({ text: "CampusEvent (AEMS)", bold: true, size: 40 }),
    ],
  })
);
children.push(
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 240 },
    children: [
      new TextRun({
        text: "Workflow Tổng Quan & Theo Từng Actor",
        bold: true,
        size: 30,
        color: "2E5BFF",
      }),
    ],
  })
);
children.push(
  p("Tài liệu mô tả luồng nghiệp vụ của hệ thống quản lý sự kiện sinh viên (AEMS). Tất cả REST endpoint đều có tiền tố /api. Phân quyền theo session (RBAC) qua AuthorizationInterceptor.")
);

// Important notes
children.push(h1("Lưu ý quan trọng (đã đối chiếu với code)"));
children.push(
  bullet("KHÔNG có bước 'mid-verify' trong luồng vận hành thực tế. Luồng check-in chính thức là Google Form QR check-in (CheckinController): sinh viên chỉ check-in một lần (kèm quiz nếu có).")
);
children.push(
  bullet("Tham gia / check-in sự kiện KHÔNG cộng điểm vào User.totalPoints. Check-in chỉ ghi participationScore trên bản ghi Attendance (thống kê mức độ tham gia), không cộng vào điểm tích lũy.")
);
children.push(p([new TextRun({ text: "Điểm tích lũy (totalPoints) chỉ phát sinh từ:", bold: true })]));
children.push(
  buildTable(
    ["Hành động", "Điểm", "activityType", "Nguồn"],
    [
      ["Đăng nhập bằng email", "+2", "EMAIL_LOGIN", "AuthService"],
      ["Đăng nhập bằng Google", "+5", "GOOGLE_LOGIN", "OAuth2LoginSuccessHandler"],
      ["Đăng ký tài khoản", "+10", "EMAIL_REGISTER / GOOGLE_REGISTER", "AuthService"],
      ["Đăng ký sự kiện", "+5", "REGISTER_EVENT", "StudentController"],
      ["Gửi feedback", "+8", "FEEDBACK", "StudentController"],
    ]
  )
);
children.push(p(""));

// 1. Actors
children.push(h1("1. Các Actor trong hệ thống"));
children.push(
  buildTable(
    ["Actor", "Vai trò chính"],
    [
      ["ADMIN", "Quản trị hệ thống: users, roles, departments, reports, email logs"],
      ["MANAGER (Trưởng đơn vị / HEAD)", "Toàn bộ Department Console: Dashboard, Proposals, Events, Reports, AEMS Toolkit (QR check-in/check-out); xem toàn khoa"],
      ["DEPARTMENT (Nhân viên đơn vị / STAFF)", "Chỉ tạo & chỉnh sửa Proposals (UI giới hạn ở Proposals)"],
      ["COMMITTEE (Hội đồng duyệt)", "Duyệt / từ chối / yêu cầu chỉnh sửa proposal"],
      ["STUDENT (Sinh viên)", "Đăng ký, check-in, làm quiz, gửi feedback"],
      ["SYSTEM (Schedulers)", "Gửi thư mời, tự đóng sự kiện, đánh vắng"],
    ]
  )
);
children.push(p(""));

// 2. Overall
children.push(h1("2. Workflow tổng thể (End-to-End)"));
children.push(imageParagraph("01-overall.png"));

// 3. Per actor
children.push(h1("3. Workflow theo từng Actor"));
children.push(
  note("Mỗi sơ đồ dưới đây chỉ mô tả hành động của chính role đó, không lồng ghép thao tác của role khác.")
);

children.push(h2("3.1. ADMIN (Quản trị hệ thống)"));
children.push(imageParagraph("02-admin.png"));
children.push(note("ADMIN có toàn quyền, bao gồm các route ADMIN-only (/users, /roles, /registrations, /feedback, /reports, ...)."));

children.push(h2("3.2. MANAGER (Trưởng đơn vị / Trưởng khoa — HEAD)"));
children.push(imageParagraph("03-manager.png"));
children.push(note("MANAGER (vị trí HEAD) nhìn được toàn khoa và có đủ 5 khu vực trong Department Console: Dashboard, Proposals, Events, Reports, AEMS Toolkit. Nhân sự khoa (DEPARTMENT/STAFF) chỉ thấy Proposals."));

children.push(h2("3.3. DEPARTMENT (Nhân viên đơn vị — STAFF)"));
children.push(imageParagraph("04-department.png"));
children.push(note("Nhân sự khoa (STAFF) chỉ nhìn chuyên ngành của mình và chỉ thao tác Proposals. Việc quản lý Event / Reports / AEMS Toolkit thuộc về MANAGER (HEAD)."));

children.push(h2("3.4. COMMITTEE (Hội đồng duyệt)"));
children.push(imageParagraph("05-committee.png"));
children.push(note("Committee chỉ thao tác được trên proposal đang ở trạng thái PENDING hoặc REVISION."));

children.push(h2("3.5. STUDENT (Sinh viên)"));
children.push(imageParagraph("06-student.png"));

children.push(h2("3.6. SYSTEM (Tiến trình tự động — không phải role người dùng)"));
children.push(imageParagraph("07-system.png"));

// 4. State lifecycle
children.push(h1("4. Vòng đời các trạng thái (State Lifecycle)"));
children.push(imageParagraph("08-state.png"));
children.push(
  note("Trạng thái Attendance trong luồng thực tế chỉ gồm CHECKED_IN (đã check-in) và ABSENT (vắng mặt, do hệ thống tự đánh khi auto-close). Không có bước trung gian mid-verify.")
);

// 5. Points summary
children.push(h1("5. Tóm tắt nguồn điểm tích lũy (gamification)"));
children.push(imageParagraph("09-points.png"));

const doc = new Document({
  creator: "CampusEvent",
  title: "CampusEvent Workflow",
  styles: {
    default: {
      document: { run: { font: "Calibri", size: 22 } },
    },
  },
  sections: [
    {
      properties: {
        page: { margin: { top: 1134, bottom: 1134, left: 1134, right: 1134 } },
      },
      children,
    },
  ],
});

Packer.toBuffer(doc).then((buf) => {
  try {
    fs.writeFileSync(outFile, buf);
    console.log("Word file written to", outFile);
  } catch (err) {
    if (err.code === "EBUSY" || err.code === "EPERM") {
      const stamp = new Date().toISOString().replace(/[:.]/g, "-").slice(0, 19);
      const alt = outFile.replace(/\.docx$/, `-${stamp}.docx`);
      fs.writeFileSync(alt, buf);
      console.log(
        "Main file is locked (đang mở trong Word). Wrote a new copy to",
        alt
      );
    } else {
      throw err;
    }
  }
});
