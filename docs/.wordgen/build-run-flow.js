const fs = require("fs");
const path = require("path");
const {
  Document,
  Packer,
  Paragraph,
  TextRun,
  HeadingLevel,
  Table,
  TableRow,
  TableCell,
  WidthType,
  AlignmentType,
  BorderStyle,
} = require("docx");

const outFile = path.join(__dirname, "..", "CampusEvent-Run-Flow.docx");

function h1(text) {
  return new Paragraph({ text, heading: HeadingLevel.HEADING_1, spacing: { before: 260, after: 120 } });
}
function h2(text) {
  return new Paragraph({ text, heading: HeadingLevel.HEADING_2, spacing: { before: 200, after: 100 } });
}
function p(text, opts = {}) {
  return new Paragraph({ children: [new TextRun({ text, ...opts })], spacing: { after: 120 } });
}
function bullet(text) {
  return new Paragraph({ text, bullet: { level: 0 }, spacing: { after: 60 } });
}
function lead(label, text) {
  return new Paragraph({
    spacing: { after: 100 },
    children: [new TextRun({ text: label + " ", bold: true }), new TextRun({ text })],
  });
}
// Câu chuyển tiếp giữa hai người: in nghiêng, thụt lề, màu xanh.
function handoff(text) {
  return new Paragraph({
    indent: { left: 380 },
    spacing: { after: 140 },
    children: [new TextRun({ text: "\u201C" + text + "\u201D", italics: true, color: "2E5BFF" })],
  });
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
    children: [new Paragraph({ children: [new TextRun({ text, bold: !!opts.bold })] })],
  });
}
function buildTable(headers, rows) {
  const headRow = new TableRow({ tableHeader: true, children: headers.map((hh) => tcell(hh, { bold: true })) });
  const bodyRows = rows.map((r) => new TableRow({ children: r.map((c) => tcell(c)) }));
  return new Table({ width: { size: 100, type: WidthType.PERCENTAGE }, rows: [headRow, ...bodyRows] });
}

const children = [];

// ---------- Title ----------
children.push(
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 60 },
    children: [new TextRun({ text: "CampusEvent (AEMS)", bold: true, size: 40 })],
  })
);
children.push(
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 200 },
    children: [new TextRun({ text: "Luồng chạy toàn bộ dự án — bắt đầu từ ai, tiếp theo là ai", bold: true, size: 28, color: "2E5BFF" })],
  })
);
children.push(
  p(
    "Tài liệu này chỉ ra thứ tự demo toàn bộ hệ thống từ đầu đến cuối, sắp theo đúng vòng đời một sự kiện thật. Bạn cứ đi từ trên xuống dưới: mỗi giai đoạn ghi rõ người trình bày, các use case phụ trách, việc cần làm và một câu chuyển tiếp để mời người tiếp theo. Nhờ vậy buổi demo sẽ chảy mạch lạc như một câu chuyện, không rời rạc."
  )
);
children.push(
  note("Thứ tự dưới đây đi theo vòng đời sự kiện (đề xuất → duyệt → sinh viên đăng ký → chốt danh sách → tổ chức & điểm danh → tổng kết), nên bạn Sang sẽ xuất hiện ở vài giai đoạn khác nhau — điều này bình thường vì mảng Quản trị/Hội đồng chạm vào nhiều điểm trong vòng đời.")
);

// ---------- Thành viên ----------
children.push(h1("Nhóm & mảng phụ trách"));
children.push(
  buildTable(
    ["Người", "Mảng phụ trách", "Use case"],
    [
      ["Anh (AnhNVT)", "Xác thực & Bảo mật: đăng nhập, đăng ký + OTP, quên/đặt lại mật khẩu, Google OAuth, phân quyền", "UC01–UC12"],
      ["Sang (SangTM)", "Quản trị hệ thống, Sự kiện, Đề xuất & Hội đồng, Thống kê & Phân tích AI", "UC31–UC49 (gồm đề xuất/hội đồng UC38–UC43)"],
      ["Đạt (DatHVT)", "Sinh viên: khám phá sự kiện, xếp hạng ưu tiên (RBL), đăng ký", "UC13–UC21"],
      ["Tú (TuHNC)", "Điểm danh & Quiz: QR check-in, mid-session, check-out, quiz, feedback", "UC22–UC30"],
    ]
  )
);

// ---------- Thứ tự tổng quan ----------
children.push(h1("Thứ tự chạy demo (nhìn nhanh)"));
children.push(
  buildTable(
    ["Bước", "Người trình bày", "Giai đoạn", "Use case chính", "Chuyển cho ai"],
    [
      ["1", "Anh", "Vào hệ thống: đăng nhập & phân quyền", "UC01–UC12", "→ Sang"],
      ["2", "Sang", "Khoa đề xuất sự kiện + Hội đồng duyệt & publish", "UC38–UC43, UC30", "→ Đạt"],
      ["3", "Đạt", "Sinh viên khám phá, ưu tiên RBL & đăng ký", "UC13–UC21", "→ Sang"],
      ["4", "Sang", "Đóng đăng ký & gửi thư mời (tính năng mới)", "UC34, UC35, UC46", "→ Tú"],
      ["5", "Tú", "Ngày diễn ra: điểm danh, quiz, feedback", "UC22–UC29", "→ Sang"],
      ["6", "Sang", "Quản trị, báo cáo & phân tích AI (tổng kết)", "UC31–UC33, UC36, UC37, UC44–UC49", "Kết thúc"],
    ]
  )
);
children.push(p("Sơ đồ chuyền tay: Anh → Sang → Đạt → Sang → Tú → Sang.", { bold: true }));

// ---------- Bước 1 ----------
children.push(h1("Bước 1 — Anh: Vào hệ thống (đăng nhập & phân quyền)"));
children.push(lead("Người trình bày:", "Anh (AnhNVT)."));
children.push(lead("Use case:", "UC03 kiểm tra cấu hình OAuth · UC05–UC06 gửi OTP & đăng ký tài khoản · UC01 đăng nhập email/mật khẩu · UC02 đăng nhập Google · UC04 hoàn tất hồ sơ cho user Google mới · UC07–UC09 quên/xác minh/đặt lại mật khẩu · UC10 tra cứu khoa–ngành · UC11 đăng xuất · UC12 phân quyền (RBAC)."));
children.push(h2("Việc cần làm"));
children.push(bullet("Mở trang đăng nhập; đăng ký một tài khoản mới bằng OTP (hoặc dùng tài khoản đã có) để cho thấy luồng xác thực."));
children.push(bullet("Cho xem đăng nhập bằng Google, và giải thích mỗi vai trò (Admin / Khoa / Hội đồng / Sinh viên) sẽ vào một màn hình khác nhau — đó chính là phân quyền."));
children.push(handoff("Em đã tạo tài khoản và phân quyền theo vai trò xong. Giờ mời Sang bắt đầu từ phía Khoa để đề xuất một sự kiện mới."));

// ---------- Bước 2 ----------
children.push(h1("Bước 2 — Sang: Khoa đề xuất + Hội đồng duyệt & publish"));
children.push(lead("Người trình bày:", "Sang (SangTM)."));
children.push(lead("Use case:", "UC38 tạo & gửi đề xuất (chọn địa điểm chuẩn + cảnh báo trùng phòng — tính năng mới) · UC40 soạn quiz trong đề xuất · UC39 sửa/xóa đề xuất · UC41 hội đồng xem & theo dõi · UC42 duyệt/từ chối/yêu cầu sửa · UC43 publish thành Event · UC30 copy quiz sang sự kiện."));
children.push(h2("Việc cần làm"));
children.push(bullet("Vào màn Khoa (Department Console) → 'Đề xuất mới' → chọn địa điểm từ danh sách phòng, đặt thời gian; nếu trùng phòng sẽ hiện cảnh báo → gửi đề xuất."));
children.push(bullet("Đổi sang tài khoản Hội đồng → xem đề xuất → duyệt → publish. Sự kiện chuyển sang PUBLISHED và hiển thị cho sinh viên."));
children.push(handoff("Sự kiện đã được duyệt và publish. Mời Đạt vào vai sinh viên để khám phá và đăng ký sự kiện này."));

// ---------- Bước 3 ----------
children.push(h1("Bước 3 — Đạt: Sinh viên khám phá, ưu tiên RBL & đăng ký"));
children.push(lead("Người trình bày:", "Đạt (DatHVT)."));
children.push(lead("Use case:", "UC13 trang landing công khai · UC15 duyệt & tìm kiếm sự kiện · UC16 gợi ý & xếp hạng ưu tiên RBL (0.4M + 0.3S + 0.2P + 0.1T) · UC17 xem chi tiết & bóc tách điểm M/S/P/T · UC18 đăng ký (REGISTERED/WAITLIST) · UC19 hủy đăng ký (đôn waitlist) · UC20 xem vé của tôi · UC14 hồ sơ cá nhân · UC21 bảng xếp hạng điểm."));
children.push(h2("Việc cần làm"));
children.push(bullet("Đăng nhập sinh viên → xem danh sách gợi ý đã được xếp theo điểm ưu tiên; mở chi tiết một sự kiện và chỉ vào phần bóc tách điểm M/S/P/T."));
children.push(bullet("Đăng ký sự kiện → xem trạng thái và mã vé trong 'Đăng ký của tôi'."));
children.push(handoff("Nhiều sinh viên đã đăng ký rồi. Trước ngày diễn ra, mời Sang chốt danh sách và gửi thư mời cho mọi người."));

// ---------- Bước 4 ----------
children.push(h1("Bước 4 — Sang: Đóng đăng ký & gửi thư mời (tính năng mới)"));
children.push(lead("Người trình bày:", "Sang (SangTM)."));
children.push(lead("Use case:", "UC34–UC35 quản lý & đổi trạng thái sự kiện · UC46 gửi email hệ thống."));
children.push(h2("Việc cần làm"));
children.push(bullet("Vào Admin Console → danh sách sự kiện → chọn 'Đóng đăng ký & gửi thư mời' → xác nhận."));
children.push(bullet("Chuyển sang Gmail, làm mới để cho thấy thư mời vừa tới; sự kiện được gắn nhãn 'Đã đóng ĐK' và sinh viên không đăng ký thêm được nữa."));
children.push(handoff("Danh sách đã chốt và thư mời đã gửi. Đến ngày sự kiện, mời Tú lo khâu điểm danh và quiz."));

// ---------- Bước 5 ----------
children.push(h1("Bước 5 — Tú: Ngày diễn ra — điểm danh, quiz & feedback"));
children.push(lead("Người trình bày:", "Tú (TuHNC)."));
children.push(lead("Use case:", "UC22 chiếu QR động & dashboard điểm danh (kèm hiển thị MSSV khi Sync — tính năng mới) · UC23 check-in qua QR + quiz bắt buộc + walk-in · UC24 mở phiên giữa buổi & đánh vắng · UC25 xác minh giữa buổi · UC26 làm & nộp quiz check-out · UC27 check-out & tính điểm tham gia · UC28 quản lý quiz · UC29 gửi khảo sát feedback."));
children.push(h2("Việc cần làm"));
children.push(bullet("Mở AEMS Toolkit → chiếu QR động; sinh viên quét QR, làm quiz và check-in."));
children.push(bullet("Bấm 'Sync Check-IN' để hiện ngay MSSV các bạn vừa điểm danh; sau đó demo check-out (quiz) và gửi feedback."));
children.push(handoff("Sự kiện đã kết thúc với đầy đủ dữ liệu điểm danh và phản hồi. Mời Sang tổng kết bằng báo cáo và phân tích."));

// ---------- Bước 6 ----------
children.push(h1("Bước 6 — Sang: Quản trị, báo cáo & phân tích AI (tổng kết)"));
children.push(lead("Người trình bày:", "Sang (SangTM)."));
children.push(lead("Use case:", "UC31–UC33 quản lý user/vai trò/khoa · UC36 cập nhật diễn giả · UC37 quản lý đăng ký · UC44 dashboard tổng hợp · UC45 báo cáo & xuất CSV · UC47 nhật ký hoạt động · UC48 tạo & đồng bộ Google Form · UC49 thống kê phản hồi & phân tích AI."));
children.push(h2("Việc cần làm"));
children.push(bullet("Vào Admin Console → Dashboard xem số liệu tổng hợp (user, sự kiện, tỷ lệ đăng ký–điểm danh)."));
children.push(bullet("Mở Reports để xuất CSV; mở phần Phân tích AI phản hồi để cho thấy sentiment, chủ đề và khuyến nghị."));
children.push(p("Chốt buổi demo: 'Vậy là ta đã đi trọn vòng đời một sự kiện — từ đề xuất, duyệt, sinh viên đăng ký, chốt danh sách & mời, điểm danh tại chỗ, đến báo cáo và phân tích cuối cùng.'", { italics: true }));

// ---------- Mẹo ----------
children.push(h1("Mẹo để buổi demo mạch lạc"));
children.push(bullet("Trước khi chuyển người, hãy đọc câu chuyển tiếp (in nghiêng xanh) để khán giả biết vì sao sang bước tiếp theo."));
children.push(bullet("Mỗi người chuẩn bị sẵn tab & tài khoản của mình để không mất thời gian đăng nhập giữa buổi."));
children.push(bullet("Nếu thời gian ngắn, có thể gộp Bước 4 vào cuối Bước 3 (đăng ký xong đóng luôn) và rút gọn Bước 6."));

const doc = new Document({
  creator: "CampusEvent",
  title: "CampusEvent Run Flow",
  styles: { default: { document: { run: { font: "Calibri", size: 22 } } } },
  sections: [
    {
      properties: { page: { margin: { top: 1134, bottom: 1134, left: 1134, right: 1134 } } },
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
      console.log("Main file is locked (đang mở trong Word). Wrote a new copy to", alt);
    } else {
      throw err;
    }
  }
});
