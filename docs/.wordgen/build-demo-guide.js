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

const outFile = path.join(__dirname, "..", "CampusEvent-Demo-Guide.docx");

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
// Nhãn in đậm + nội dung thường trên cùng một dòng.
function lead(label, text) {
  return new Paragraph({
    spacing: { after: 100 },
    children: [new TextRun({ text: label + " ", bold: true }), new TextRun({ text })],
  });
}
// Lời thoại gợi ý: in nghiêng, thụt lề, màu xanh — để đọc/parafrase khi demo.
function quote(text) {
  return new Paragraph({
    indent: { left: 380 },
    spacing: { after: 100 },
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
  const headRow = new TableRow({
    tableHeader: true,
    children: headers.map((hh) => tcell(hh, { bold: true })),
  });
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
    children: [new TextRun({ text: "Kịch bản Demo 3 tính năng mới — đi đâu, bấm gì, nói gì", bold: true, size: 28, color: "2E5BFF" })],
  })
);
children.push(
  p(
    "Tài liệu này là kịch bản trình bày (demo script) cho 3 tính năng vừa bổ sung vào hệ thống quản lý sự kiện sinh viên (AEMS). Mục tiêu là giúp bạn dẫn dắt buổi demo một cách tự nhiên: nêu lợi ích trước, vừa thao tác vừa kể câu chuyện nghiệp vụ, thay vì đọc khô khan từng bước. Các tính năng bám sát bộ Use Case của dự án: UC38 (đề xuất & địa điểm), UC34–UC35 và UC46 (quản lý sự kiện & email hệ thống), UC22–UC23 và UC48 (điểm danh & đồng bộ Google Form)."
  )
);

// ---------- 0. Chuẩn bị ----------
children.push(h1("0. Chuẩn bị trước khi lên demo (khoảng 2 phút)"));
children.push(
  buildTable(
    ["Việc cần làm", "Chi tiết"],
    [
      ["Khởi động ứng dụng", "Chạy với profile local + SQL Server. Đợi log khởi động hiện dòng 'DemoDataSeeder: đã tạo event demo…' là dữ liệu đã sẵn sàng."],
      ["Dữ liệu demo đã có sẵn", "Sự kiện '[DEMO 7h30] Workshop Spring Boot MVC' tại Hội trường Alpha, 5 sinh viên (DEMO01–DEMO05) đã đăng ký, trong đó DEMO01/02/03 đã check-in."],
      ["Mở sẵn các tab trình duyệt", "1) Trang quản trị (Admin Console) · 2) Trang Khoa (Department Console) · 3) AEMS Toolkit (màn QR check-in) · 4) Hộp thư Gmail của bạn."],
      ["Tài khoản đăng nhập", "Tài khoản Quản trị (ví dụ admin@example.com) và tài khoản Trưởng khoa (MANAGER) mà bạn đang dùng. Có thể đăng nhập Google bằng chính Gmail của bạn."],
    ]
  )
);
children.push(
  note("Mở sẵn tab Gmail và đăng nhập trước. Khi bấm 'Đóng đăng ký & gửi thư mời', bạn chỉ cần chuyển sang tab Gmail và refresh để khán giả thấy thư mời tới ngay — đây là điểm nhấn mạnh nhất của buổi demo.")
);

// ---------- 1. Mở đầu ----------
children.push(h1("1. Mở đầu (khoảng 30 giây)"));
children.push(
  p(
    "Đừng vào thao tác ngay. Hãy đặt bối cảnh nghiệp vụ trong một câu để khán giả hiểu 'tại sao', rồi mới cho họ thấy 'như thế nào'."
  )
);
children.push(lead("Ý tưởng dẫn dắt:", "gắn 3 tính năng vào một hành trình thật của ban tổ chức sự kiện — từ lúc chốt danh sách người tham dự, đến lúc lên kế hoạch phòng ốc, và cuối cùng là điểm danh tại sự kiện."));
children.push(quote("Ba tính năng em thêm lần này đi theo đúng vòng đời một sự kiện thật: đầu tiên là chốt đăng ký và mời người tham dự, tiếp đến là chọn phòng mà không bị trùng lịch, và cuối cùng là điểm danh nhanh tại chỗ. Em sẽ demo lần lượt theo đúng thứ tự đó ạ."));

// ---------- 2. Tính năng 1 ----------
children.push(h1("2. Tính năng 1 — Đóng đăng ký & gửi thư mời tự động"));
children.push(lead("Chứng minh điều gì:", "chỉ bằng một nút bấm, ban tổ chức chốt danh sách đăng ký và gửi thư mời hàng loạt qua email cho toàn bộ sinh viên đã đăng ký."));
children.push(lead("Use case liên quan:", "UC34–UC35 (quản lý & đổi trạng thái sự kiện), UC46 (gửi email hệ thống), nối tiếp UC18 (đăng ký sự kiện)."));
children.push(h2("Cách kể khi demo"));
children.push(
  p(
    "Ở tab Admin Console, mở danh sách 'Tất cả sự kiện' và chỉ vào sự kiện demo. Trong khi trỏ chuột vào menu thao tác của sự kiện đó, hãy giải thích rằng trước đây khi hết hạn đăng ký, ban tổ chức phải tự tổng hợp danh sách rồi gửi mail thủ công cho từng người — vừa lâu vừa dễ sót. Sau đó chọn mục 'Đóng đăng ký & gửi thư mời', xác nhận, và để hệ thống làm phần còn lại. Ngay lập tức hệ thống báo đã gửi bao nhiêu thư mời và gắn nhãn 'Đã đóng ĐK' cho sự kiện."
  )
);
children.push(
  p(
    "Đây là lúc chuyển sang tab Gmail và làm mới hộp thư để cho khán giả thấy các thư mời vừa tới. Mở một email ra để khoe mẫu thư mời có đầy đủ tên sự kiện, thời gian và địa điểm."
  )
);
children.push(h2("Lời dẫn gợi ý"));
children.push(quote("Giả sử đã đến hạn chốt đăng ký. Thay vì gửi mail thủ công cho từng bạn, em chỉ cần bấm 'Đóng đăng ký và gửi thư mời'."));
children.push(quote("Hệ thống báo đã gửi 5 thư mời, và sự kiện được đánh dấu 'Đã đóng ĐK' — từ giờ sinh viên không đăng ký thêm được nữa. Mình cùng xem thư đã tới hộp thư thật chưa nhé."));
children.push(h2("Điểm nhấn nên nói"));
children.push(bullet("Thư mời gửi thật qua email (SMTP), không phải giả lập — nên khán giả thấy email tới ngay."));
children.push(bullet("Sau khi đóng, hệ thống chặn đăng ký mới ở phía máy chủ, đảm bảo danh sách đã chốt là cố định."));
children.push(bullet("Toàn bộ gửi chạy nền (bất đồng bộ) nên nút phản hồi tức thì, không bắt người dùng chờ."));

// ---------- 3. Tính năng 2 ----------
children.push(h1("3. Tính năng 2 — Chọn địa điểm chuẩn hoá & cảnh báo trùng phòng"));
children.push(lead("Chứng minh điều gì:", "người tạo đề xuất chọn phòng từ danh sách có sẵn (tránh gõ sai tên), và hệ thống cảnh báo ngay nếu phòng đó đã bị dùng trùng khung giờ."));
children.push(lead("Use case liên quan:", "UC38 (tạo & gửi đề xuất sự kiện — địa điểm, sức chứa), nối tiếp UC42–UC43 (duyệt & publish)."));
children.push(h2("Cách kể khi demo"));
children.push(
  p(
    "Sang tab Department Console (đăng nhập tài khoản Khoa), mở form 'Đề xuất mới'. Khi tới ô Địa điểm, hãy dừng lại một nhịp và giải thích: trước đây ô này gõ tay nên mỗi người viết một kiểu ('Hội trường Alpha', 'HT Alpha', 'alpha'…), rất khó kiểm soát trùng phòng. Bây giờ nó là danh sách phòng chuẩn của trường. Chọn Hội trường Alpha để khán giả thấy sức chứa tự điền theo phòng."
  )
);
children.push(
  p(
    "Tiếp theo, cố tình đặt thời gian trùng với sự kiện demo (sáng nay, khoảng 9 giờ). Ngay khi chọn xong, dòng cảnh báo đỏ hiện ra dưới ô địa điểm, nói rõ phòng đã được dùng cho sự kiện nào. Sau đó đổi sang một phòng khác (ví dụ Hội trường Beta) để khán giả thấy cảnh báo biến mất — chứng tỏ hệ thống kiểm tra theo thời gian thực."
  )
);
children.push(h2("Lời dẫn gợi ý"));
children.push(quote("Ô địa điểm giờ là danh sách phòng chuẩn của trường, nên không ai gõ sai tên nữa; chọn phòng xong sức chứa cũng tự điền theo phòng."));
children.push(quote("Nếu em chọn đúng phòng và khung giờ đang có sự kiện khác, hệ thống cảnh báo ngay là phòng đã được dùng. Em đổi sang phòng trống thì cảnh báo tự mất."));
children.push(h2("Điểm nhấn nên nói"));
children.push(bullet("Chuẩn hoá tên phòng là điều kiện để việc dò trùng lịch trở nên chính xác."));
children.push(bullet("Cảnh báo theo thời gian thực, kiểm tra chồng lấn khung giờ chứ không chỉ trùng ngày."));
children.push(bullet("Danh sách phòng được lưu trong bảng riêng (Room), có thể mở rộng thêm phòng dễ dàng."));

// ---------- 4. Tính năng 3 ----------
children.push(h1("4. Tính năng 3 — Đồng bộ điểm danh & hiển thị mã số sinh viên"));
children.push(lead("Chứng minh điều gì:", "khi bấm Sync Check-IN/Check-OUT, hệ thống hiển thị ngay mã số sinh viên (MSSV) của những người vừa điểm danh, giúp theo dõi trực quan tại sự kiện."));
children.push(lead("Use case liên quan:", "UC22 (chiếu QR & dashboard điểm danh), UC23 (check-in qua QR), UC48 (đồng bộ Google Form)."));
children.push(h2("Cách kể khi demo"));
children.push(
  p(
    "Sang tab AEMS Toolkit và mở đúng sự kiện demo. Giải thích rằng đây là màn hình ban tổ chức chiếu QR để sinh viên quét điểm danh. Trước đây bấm Sync chỉ hiện các con số tổng hợp, khó biết cụ thể ai vừa vào. Giờ hãy bấm 'Sync Check-IN' và cho khán giả thấy danh sách MSSV của những bạn đã điểm danh (DEMO01, DEMO02, DEMO03) hiện ra ngay bên dưới."
  )
);
children.push(
  p(
    "Nếu muốn ấn tượng hơn, hãy nói rằng tính năng này hoạt động kể cả khi chưa cấu hình Google — vì hệ thống đọc trực tiếp danh sách điểm danh trong cơ sở dữ liệu. Nhờ vậy buổi demo không phụ thuộc vào mạng hay tài khoản Google."
  )
);
children.push(h2("Lời dẫn gợi ý"));
children.push(quote("Sau khi các bạn quét QR để điểm danh, em bấm Sync Check-IN thì thấy ngay mã số sinh viên của những bạn vừa vào — ví dụ DEMO01, DEMO02, DEMO03."));
children.push(quote("Điểm hay là kể cả khi không có Google Form, hệ thống vẫn đọc thẳng từ cơ sở dữ liệu và hiển thị danh sách, nên demo không lo bị lỗi mạng."));
children.push(h2("Điểm nhấn nên nói"));
children.push(bullet("Hiển thị đúng MSSV người mới điểm danh, không chỉ con số tổng."));
children.push(bullet("Hoạt động độc lập với Google — đọc từ dữ liệu điểm danh thực tế của hệ thống."));
children.push(bullet("Có thể mở rộng: quét QR thêm một bạn rồi bấm Sync lại để thấy danh sách cập nhật."));

// ---------- 5. Kết & Q&A ----------
children.push(h1("5. Kết thúc & câu hỏi thường gặp"));
children.push(
  p(
    "Chốt lại bằng một câu tổng kết ngắn gọn, gắn 3 tính năng vào giá trị thực tế cho ban tổ chức. Sau đó chủ động mời câu hỏi."
  )
);
children.push(quote("Ba tính năng này giúp ban tổ chức chốt đăng ký và mời người tham dự nhanh hơn, tránh trùng phòng ngay từ khâu đề xuất, và theo dõi điểm danh trực quan tại sự kiện."));
children.push(h2("Chuẩn bị cho các câu hỏi có thể gặp"));
children.push(
  buildTable(
    ["Câu hỏi có thể bị hỏi", "Gợi ý trả lời"],
    [
      ["Thư mời gửi thật hay giả lập?", "Gửi thật qua email (SMTP đã cấu hình). Trong demo, các tài khoản dùng bí danh của cùng một hộp thư nên thư về đúng inbox để trình chiếu."],
      ["Trùng phòng có bị chặn cứng không?", "Hệ thống cảnh báo rõ ràng và vẫn cho phép người dùng chủ động xác nhận, tránh khoá cứng gây bất tiện khi thật sự cần."],
      ["Không có Google thì điểm danh thế nào?", "Sinh viên vẫn check-in qua QR nội bộ; nút Sync đọc danh sách điểm danh thẳng từ cơ sở dữ liệu nên luôn hiển thị được MSSV."],
      ["Vì sao ưu tiên đăng ký (RBL) lại quan trọng?", "Gắn với UC16: hệ thống xếp hạng theo ngành/kỳ/điểm/thời gian, nên danh sách chốt khi 'đóng đăng ký' đã phản ánh đúng thứ tự ưu tiên."],
    ]
  )
);

// ---------- 6. Mẹo trình bày ----------
children.push(h1("6. Mẹo trình bày tự nhiên (tránh máy móc)"));
children.push(bullet("Nói lợi ích trước, thao tác sau: mỗi tính năng mở đầu bằng 'vấn đề trước đây', rồi mới bấm nút."));
children.push(bullet("Đừng đọc từng bước 'bấm cái này, rồi bấm cái kia'. Hãy kể như đang xử lý công việc thật của ban tổ chức."));
children.push(bullet("Dùng câu chuyện tình huống: 'Giả sử đã hết hạn đăng ký…', 'Giả sử hai khoa cùng muốn mượn Hội trường Alpha…'."));
children.push(bullet("Giữ nhịp: mỗi tính năng khoảng một phút; dừng một nhịp ở khoảnh khắc 'wow' (email tới, cảnh báo đỏ, MSSV hiện ra)."));
children.push(bullet("Chuẩn bị sẵn các tab và đăng nhập trước để không mất thời gian gõ mật khẩu giữa buổi."));

// ---------- 7. Kịch bản nhanh ----------
children.push(h1("7. Kịch bản nhanh 3 phút (bản rút gọn để cầm tay)"));
children.push(
  buildTable(
    ["Phút", "Màn hình", "Việc làm", "Câu chốt"],
    [
      ["0:00", "—", "Mở đầu, đặt bối cảnh vòng đời sự kiện.", "'Ba tính năng đi theo vòng đời một sự kiện thật.'"],
      ["0:30", "Admin Console", "Bấm 'Đóng đăng ký & gửi thư mời' → chuyển sang Gmail xem thư.", "'Chốt danh sách và mời người tham dự chỉ bằng một nút.'"],
      ["1:30", "Department Console", "Đề xuất mới → chọn Hội trường Alpha, đặt giờ trùng → cảnh báo đỏ.", "'Chọn phòng chuẩn và tránh trùng lịch ngay từ đầu.'"],
      ["2:30", "AEMS Toolkit", "Bấm Sync Check-IN → hiện MSSV DEMO01/02/03.", "'Theo dõi ai vừa điểm danh, ngay tại chỗ.'"],
      ["3:00", "—", "Tổng kết + mời câu hỏi.", "'Nhanh hơn, không trùng phòng, điểm danh trực quan.'"],
    ]
  )
);

const doc = new Document({
  creator: "CampusEvent",
  title: "CampusEvent Demo Guide",
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
