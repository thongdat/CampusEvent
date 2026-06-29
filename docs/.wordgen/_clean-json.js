const fs = require("fs");
const path = require("path");
const raw = fs.readFileSync(path.join(__dirname, "rbl-event34-data.raw"), "utf8");
const body = raw.replace(/\r?\n/g, "");
const start = body.indexOf("{");
const end = body.lastIndexOf("}");
if (start < 0 || end < 0) { console.error("Không tìm thấy JSON"); process.exit(1); }
const data = JSON.parse(body.slice(start, end + 1));
// event được FOR JSON WITHOUT_ARRAY_WRAPPER trả ra dưới dạng chuỗi escape → parse lại.
if (typeof data.event === "string") data.event = JSON.parse(data.event);
fs.writeFileSync(path.join(__dirname, "rbl-event34-data.json"), JSON.stringify(data, null, 2), "utf8");
console.log("OK event=", data.event ? data.event.title : null,
  "| cap=", data.event ? data.event.capacity : null,
  "| ranking=", (data.ranking || []).length,
  "| registered=", (data.registered || []).length,
  "| waitlist=", (data.waitlist || []).length);
