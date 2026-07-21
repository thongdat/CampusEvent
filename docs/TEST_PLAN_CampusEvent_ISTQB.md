# TEST PLAN — CampusEvent (AEMS)

**Theo mẫu ISTQB Foundation Level v4.0**

---

## DOCUMENT INFORMATION

| Field | Description |
|---|---|
| **Project Name** | CampusEvent / AEMS (Academic Event Management System) |
| **Document Title** | TEST PLAN |
| **Version** | v1.0 |
| **Prepared By** | QA Team – CampusEvent |
| **Reviewed By** | Project Manager / Team Lead |
| **Approved By** | Stakeholder / Mentor |
| **Date** | 2026-07-19 |
| **Status** | Draft |
| **Related system** | Spring Boot campus event lifecycle (proposal → publish → RBL register → invitation → check-in → feedback → report) |
| **Repository** | `https://github.com/thongdat/CampusEvent` |

---

## REVISION HISTORY

| Version | Date | Author | Description |
|---|---|---|---|
| 1.0 | 2026-07-19 | QA Team | Initial Test Plan for CampusEvent based on ISTQB v4.0 template |

---

## 1. INTRODUCTION

### Purpose

Tài liệu Test Plan này định nghĩa **phạm vi, chiến lược, môi trường, lịch trình, nguồn lực, công cụ và tiêu chí vào/ra** cho hoạt động kiểm thử hệ thống **CampusEvent (AEMS)**.

### What this plan covers

- Mục tiêu kiểm thử và giá trị nghiệp vụ
- Phạm vi In Scope / Out of Scope
- Các loại kiểm thử, cách tiếp cận, kỹ thuật thiết kế test
- Môi trường, dữ liệu, vai trò, lịch, quản lý defect, rủi ro
- Deliverables và kế hoạch giao tiếp
- Phụ lục: mẫu Test Case, Defect Report, Decision Table, Use Case Testing gắn với nghiệp vụ AEMS

### Why testing matters for this project

CampusEvent quản lý toàn bộ vòng đời sự kiện học thuật/ngoại khóa: đề xuất → duyệt → công bố → **xếp chỗ theo điểm ưu tiên (RBL)** → thư mời → check-in QR → quiz/feedback → báo cáo. Lỗi ở các luồng này dễ gây:

- Sai suất tham dự / mất công bằng khi cấp chỗ (RBL)
- Gửi email sai hoặc trùng
- Check-in sai trạng thái điểm danh
- Lộ dữ liệu giữa các vai trò (Student / Department / Committee / Admin)

Kiểm thử nhằm đảm bảo hệ thống **đúng nghiệp vụ, ổn định, an toàn quyền truy cập** trước khi demo / release.

**Example statement (project-specific):**

> This Test Plan defines the testing approach, scope, schedule, resources, tools, and activities for **CampusEvent (AEMS)**. The objective is to ensure functional and non-functional requirements are met before demo/production release on Render + Neon PostgreSQL.

---

## 2. PROJECT OVERVIEW

### System overview

**CampusEvent (AEMS)** là hệ thống quản lý sự kiện sinh viên cho campus FPT. Hệ thống hỗ trợ nhiều vai trò:

| Role | Trách nhiệm chính |
|---|---|
| **STUDENT** | Xem sự kiện, đăng ký (RBL), xem vé, check-in, feedback, leaderboard |
| **DEPARTMENT / MANAGER** | Tạo đề xuất sự kiện, chọn phòng, theo dõi proposal/event khoa |
| **COMMITTEE** | Duyệt / từ chối / yêu cầu chỉnh sửa đề xuất |
| **ADMIN** | Quản trị user, khoa, phòng, event, đăng ký, email, báo cáo, đóng đăng ký |

### Main business processes

1. Khoa tạo **Proposal** (có địa điểm từ danh mục **Room**)
2. Hội đồng **Approve / Reject / Revise**
3. Event được **Publish**
4. Sinh viên **Register** theo cơ chế **RBL** (không FIFO thuần)
5. Hệ thống gửi **thư mời ~7 ngày trước** sự kiện
6. Admin có thể **Đóng đăng ký** + gửi email thông báo (tiện demo)
7. Check-in / checkout (QR / Google Form sync)
8. Event **tự đóng** sau giờ kết thúc + grace 30 phút → `COMPLETED` + đánh vắng
9. Feedback / quiz / báo cáo

### Key modules

| Module | Mô tả ngắn |
|---|---|
| Authentication | Login, register, forgot/reset password, Google OAuth (optional) |
| Student Portal | Events, RBL register/cancel, my registrations, feedback |
| Department Portal | Proposal CRUD, organizers, quiz trong proposal |
| Committee Portal | Approve / reject / revise proposals |
| Admin Console | Users, roles, departments, rooms, events, proposals, registrations, email, logs, reports |
| RBL Priority | Tính điểm `0.4·M + 0.3·S + 0.2·P + 0.1·T`, demote/promote slot |
| Invitation & Email | Scheduler 7 ngày + gửi thủ công + email đóng đăng ký |
| Attendance / Check-in | QR token, Google Form sync, mark absent |
| Rooms | Danh mục phòng sự kiện (CRUD, search, pagination) |

### Technologies

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 2.7.14, Spring Security, Spring Data JPA, Spring Mail / Brevo |
| Frontend | HTML / CSS / JavaScript tĩnh (`src/main/resources/static`) |
| API prefix | Context-path `/api` |
| Local DB (default runtime) | PostgreSQL (`campus_event`) + `schema-postgresql.sql` |
| Legacy / alternate local | SQL Server `event_management_db` (scripts & reconcile docs) |
| Deploy DB | Neon PostgreSQL |
| Deploy platform | Render (Docker, profile `render`) |
| Build / CI | Maven, GitHub Actions (`mvn clean verify`) |

### Users (seed / demo)

- Admin: `aems.admin01@uni.edu.vn`
- Department: `dept01@uni.edu.vn` …
- Committee: `committee01@uni.edu.vn` …
- Student: `student001@uni.edu.vn` …
- Default local seed password: `Campus@2026` (override bằng `DEMO_*_PASSWORD` trên Render)

---

## 3. TEST OBJECTIVES

1. **Verify functional requirements** của các module Auth, Proposal, Event, RBL Registration, Invitation, Check-in, Rooms, Admin.
2. **Validate business workflows** end-to-end theo từng vai trò.
3. **Detect defects early** trước demo/release (đặc biệt RBL, capacity, ticket sync).
4. **Ensure system stability** trên môi trường local và Render (cold start Neon).
5. **Verify security / authorization**: phân quyền 4 vai trò, session / `X-User-Email`, không truy cập chéo dữ liệu.
6. **Validate integrations**: email (SMTP/Brevo), Google OAuth/Forms (khi cấu hình), DB PostgreSQL/Neon.
7. **Confirm non-functional basics**: health check, response chấp nhận được khi Neon warm, UI usable trên Chrome/Edge.

**Project-specific examples:**

- Ensure critical business functions operate correctly (register, approve, close registration, auto-close).
- Verify role-based access for Student / Department / Committee / Admin.
- Ensure APIs under `/api` respond within acceptable limits after warm-up.
- Validate frontend static screens integrate correctly with backend JSON APIs.

---

## 4. TEST SCOPE

### 4.1 In Scope

| Module | Description | Priority |
|---|---|---|
| Authentication | Login, logout, register, forgot/reset password; OAuth Google (nếu env đủ) | Critical |
| Student – Events & RBL | List/detail, register, demote lowest score, waitlist, cancel + auto-promote, ticket | Critical |
| Close registration | Admin đóng ĐK, chặn register mới, gửi email REGISTERED | Critical |
| Invitation (7 days) | Scheduler + manual `send-invitations`; `invitationSentAt` | High |
| Proposal workflow | Department tạo/sửa; Committee approve/reject/revise; publish event | Critical |
| Rooms | Admin CRUD phòng; form proposal/event chọn dropdown phòng | High |
| Event lifecycle | Publish, status change, auto-close + mark absent (grace 30’) | High |
| Attendance / Check-in | QR/check-in submit, trạng thái điểm danh | High |
| Admin users/roles/depts | CRUD, search, pagination (users), lock/unlock | High |
| Registrations / Feedback / Email logs / Reports | Xem, lọc, thống kê cơ bản | Medium |
| Deploy smoke | `/api/auth/test`, landing, login trên Render | High |

### 4.2 Out of Scope (current cycle)

| Item | Reason |
|---|---|
| Mobile native apps (iOS/Android) | Hệ thống hiện là web |
| Full performance load testing (thousands of concurrent users) | Không đủ hạ tầng; chỉ smoke/perf nhẹ |
| Penetration testing chuyên sâu | Ngoài phạm vi học phần/demo |
| AI / facial recognition attendance | Không có trong product |
| Third-party CRM / payment gateway | Không tích hợp |
| Complete UI automation suite Selenium/Playwright | Chưa có; ưu tiên manual + unit hiện có |
| Exact pixel-perfect cross-browser visual QA | Chỉ Chrome + Edge chính |

---

## 5. TEST ITEMS

| Item | Description | Location / Entry |
|---|---|---|
| Login / Auth API | `/api/auth/login`, register, OTP/reset | `AuthController`, `login.html` |
| Student API | `/api/student/*` | `StudentController`, `screen-student.html` |
| Committee API | `/api/committee/proposals/{id}/approve|reject|revise` | `CommitteeController`, `screen-committee.html` |
| Admin API | `/api/admin/*` | `AdminDashboardController`, `admin-screen/` |
| RBL engine | Priority score + capacity demote/promote | `PriorityRankingService` |
| Invitation scheduler | 7-day lead emails | `InvitationScheduler` |
| Close registration | `/api/admin/events/{id}/close-registration` | Admin Events menu |
| Auto-close scheduler | End + 30 minutes → COMPLETED | `EventAutoCloseScheduler` |
| Rooms API / UI | `/api/admin/rooms`, `rooms.html` | Admin → Phòng sự kiện |
| Check-in | `/api/checkin/*`, `checkin.html` | Check-in flow |
| PostgreSQL schema | `schema-postgresql.sql` | Local + Neon |
| Email service | Brevo / SMTP | `EmailService` |
| Google Forms (optional) | Auto-create / sync | `GoogleFormsApiService` |
| Seed / QA data | Demo accounts + `[QA-TEST]` dataset | `DataSeeder`, `docs/test-data.sql` |
| Existing unit tests | Auth, Committee, Registration, Google Forms, OAuth | `src/test/java/...` |

---

## 6. TEST TYPES

| Test Type | Purpose | Applied to CampusEvent |
|---|---|---|
| **Functional Testing** | Validate business functions | Login, RBL register, approve proposal, rooms, close registration |
| **Integration Testing** | Verify module communication | Student API ↔ DB; Admin close-reg ↔ EmailService; Committee approve ↔ Event create |
| **System Testing** | End-to-end workflows | Khoa tạo proposal → duyệt → SV đăng ký → đóng ĐK → email |
| **Regression Testing** | Changes do not break existing features | After each sprint / before demo; `mvn test` + critical manual suite |
| **Smoke / Sanity** | Build deployable | `/api/auth/test`, login 4 roles, open landing |
| **Security Testing (basic)** | AuthZ / AuthN | Wrong role cannot access other portals; closed registration blocks register |
| **Usability Testing** | User friendliness | Dropdown phòng thay vì nhập tay; confirm dialog ngắn gọn |
| **Compatibility Testing** | Browser | Chrome, Edge (latest); desktop viewport chính |
| **Performance Testing (light)** | Cold start awareness | Render+Neon first request; dashboard overview cache |
| **Unit Testing** | Developer-level | Existing JUnit tests under `src/test` |

**Note:** Regression testing will be executed after every significant merge to `main` / before demo day.

---

## 7. TEST APPROACH / TEST STRATEGY

### Levels

| Testing Level | Approach | Owner |
|---|---|---|
| Unit Testing | JUnit + Mockito (`mvn test`) | Developers |
| Integration Testing | API testing with Postman / curl against local `/api` | QA + Dev |
| System Testing | Manual E2E theo test cases & use cases | QA |
| Acceptance / Demo readiness | Checklist smoke trên Render | Team + Mentor |

### Overall approach

- **Agile / iterative**: test theo sprint / feature (RBL, Rooms, Close registration…)
- **Risk-based**: ưu tiên RBL, capacity/ticket, email, phân quyền
- **Black-box** cho UI/API nghiệp vụ; **white-box** cho unit (scheduler, ranking formula)
- **Manual** là chính cho E2E; **automation** hiện có ở unit layer

### Design techniques (ISTQB)

| Technique | Where applied |
|---|---|
| Equivalence Partitioning | Login valid/invalid; capacity full/not full |
| Boundary Value Analysis | Capacity = 0/1/N; semester 1–9; registration window edges |
| Decision Table | Login credentials; RBL demote when over capacity |
| Use Case Testing | Approve proposal; Student register; Close registration |
| State Transition | Proposal PENDING→APPROVED/REJECTED/REVISION; Event PUBLISHED→COMPLETED; Registration REGISTERED↔WAITLIST |

### Automation policy (current)

- Run `mvn clean verify` in CI
- Expand unit/integration for RBL & close-registration in later iterations
- UI automation (Selenium/Playwright) = future backlog (Out of Scope this cycle)

---

## 8. ENTRY CRITERIA

Testing bắt đầu khi:

1. Requirements / feature list được team thống nhất (README + Use Case phân công)
2. Build Maven thành công (`mvn -DskipTests package` hoặc `verify`)
3. Test environment sẵn sàng:
   - Local: PostgreSQL `campus_event` **hoặc** SQL Server legacy đã seed
   - Deploy: Render service Live + Neon active
4. Test data sẵn sàng:
   - Seed demo accounts
   - (Khuyến nghị) chạy `docs/test-data.sql` cho dataset `[QA-TEST]`
5. Test cases critical đã review
6. Email config sẵn (Brevo/SMTP) nếu test invitation / close-registration email
7. Không còn blocker Severity Critical trên môi trường test

---

## 9. EXIT CRITERIA

Testing dừng / sẵn sàng demo khi:

1. **100%** test case **Critical** đã executed
2. **≥ 95%** test case **High** đã executed
3. **Không còn Severity 1 (Critical)** defect mở
4. Severity High còn mở đã có workaround được chấp nhận cho demo
5. Smoke trên Render pass: health, login 4 roles, 1 luồng RBL hoặc close-registration demo
6. Test Summary Report hoàn tất
7. (Nếu có) UAT / mentor sign-off

---

## 10. TEST ENVIRONMENT

### 10.1 Local environment

| Component | Configuration |
|---|---|
| OS | Windows 10/11 |
| JDK | 17+ |
| Build | Maven 3.9.x (`apache-maven-3.9.9` in repo) |
| Backend | Spring Boot 2.7.14, port `8080`, context `/api` |
| Frontend | Static resources served by Spring |
| Database (primary) | PostgreSQL 14/15/16 — DB `campus_event` |
| Database (legacy option) | SQL Server — `event_management_db` |
| Schema init | `schema-postgresql.sql` (`spring.sql.init.mode=always`) |
| Browsers | Chrome, Edge (latest) |

### 10.2 Deploy / staging-like environment

| Component | Configuration |
|---|---|
| Hosting | Render (Docker), region Singapore |
| Profile | `SPRING_PROFILES_ACTIVE=render` |
| Database | Neon PostgreSQL (`sslmode=require`) |
| Email | Brevo API (`BREVO_API_KEY`, sender verified) and/or SMTP |
| Health | `GET /api/auth/test` |
| Public URLs | `/api/landing.html`, `/api/login.html` |

### 10.3 Known environment constraints

- Render free / Neon free có **cold start** → request đầu chậm (không coi là functional fail nếu warm lại pass)
- Google OAuth/Forms chỉ test khi đủ `GOOGLE_CLIENT_ID/SECRET` và redirect URI đúng

---

## 11. TEST TOOLS

| Tool | Purpose |
|---|---|
| **JUnit 5 + Mockito** | Unit / controller tests |
| **Maven** | Build & test execution |
| **GitHub Actions** | CI (`mvn clean verify`) |
| **Postman / curl** | API testing |
| **Browser DevTools** | UI debugging, network, console |
| **pgAdmin / DBeaver / SSMS** | DB verification |
| **GitHub Issues / Excel / Notion** *(team choice)* | Defect tracking |
| **Brevo dashboard** | Verify outbound emails |
| **Render Logs / Neon console** | Deploy troubleshooting |

*(Selenium / JMeter / Katalon: optional future — currently Out of Scope)*

---

## 12. TEST DATA MANAGEMENT

### Principles

- Dùng **seed demo** cho login nhanh
- Dùng dataset **`[QA-TEST]`** (`docs/test-data.sql`) cho nghiệp vụ deterministic (capacity nhỏ, waitlist, check-in hôm nay…)
- Không dùng dữ liệu cá nhân thật trên môi trường public
- Tách dataset **positive / negative / boundary**
- Cleanup-safe: prefix `[QA-TEST]` để không đụng data demo `[AEMS]`

### Sample datasets

| Dataset | Purpose |
|---|---|
| Valid Accounts (4 roles) | Positive login & portal access |
| Invalid Accounts | Wrong password, locked user, missing email header |
| RBL Contenders | Students khác major/semester/points để so điểm ưu tiên |
| Capacity-full Event | Event sức chứa nhỏ để test demote → WAITLIST |
| Closed-registration Event | Sau khi admin đóng ĐK |
| Invitation Window Event | `startTime` trong 7 ngày tới, `invitationSentAt = null` |
| Rooms catalog | Alpha101, Hội Trường Alpha, Phòng Lab, Phòng Luk (+ custom) |
| Large `[AEMS]` seed | Performance/UI stress nhẹ (không phải load test) |

### Data creation methods

1. `DataSeeder` / `RoomSeeder` khi `APP_SEED_ENABLED=true`
2. SQL: `docs/test-data.sql`
3. Manual qua UI Admin / Department
4. RBL reconcile scripts (SQL Server) chỉ khi cần sửa DB demo — **không** chạy bừa trên Neon prod

---

## 13. ROLES AND RESPONSIBILITIES

| Role | Responsibility |
|---|---|
| **QA Lead** | Sở hữu Test Plan, ưu tiên risk-based, báo cáo tuần, exit criteria |
| **Tester / QA Member** | Viết & execute test cases, log defects, retest |
| **Developer** | Unit tests, fix defects, hỗ trợ root-cause |
| **Project Manager / Team Lead** | Approve scope/schedule, quyết định go/no-go demo |
| **Customer / Mentor** | UAT / nhận demo, feedback chấp nhận |
| **DevOps (team)** | Render/Neon env vars, deploy, logs |

---

## 14. TEST SCHEDULE

> Điều chỉnh ngày cụ thể theo lịch môn học / demo. Dưới đây là khung gợi ý.

| Phase | Start | End | Owner | Notes |
|---|---|---|---|---|
| Test Planning | T0 | T0+2d | QA Lead | Tài liệu này |
| Test Case Design | T0+2d | T0+6d | QA | Ưu tiên Critical/High |
| Environment Setup | T0+2d | T0+4d | Dev + QA | Local PG + Render smoke |
| Test Data Prep | T0+3d | T0+5d | QA | Seed + `[QA-TEST]` |
| Test Execution (Cycle 1) | T0+6d | T0+12d | QA | Functional + E2E |
| Defect Fix / Retest | T0+8d | T0+14d | Dev + QA | Parallel |
| Regression | T0+13d | T0+15d | QA | Critical suite |
| UAT / Demo rehearsal | T0+15d | T0+16d | Team + Mentor | Render |
| Test Closure | T0+16d | T0+17d | QA Lead | Summary + sign-off |

---

## 15. DEFECT MANAGEMENT PROCESS

### Workflow

```text
New → Assigned → In Progress → Fixed → Retest → Closed
                 ↘ Rejected / Duplicate / Won’t Fix (with reason)
                 ↘ Reopened (if retest fails)
```

### Severity

| Severity | Description | Example in CampusEvent |
|---|---|---|
| **Critical (S1)** | Crash / data loss / security break | Sai quyền Admin; mất toàn bộ registration; DB không kết nối prod |
| **High (S2)** | Major business function fails | RBL không demote khi over capacity; approve không tạo event; đóng ĐK không chặn register |
| **Medium (S3)** | Partial function / workaround exists | Email fail nhưng ĐK vẫn đóng; UI badge sai nhưng API đúng |
| **Low (S4)** | Cosmetic / minor UX | Text dài, spacing, copy confirm dialog |

### Priority

| Priority | Meaning |
|---|---|
| **P1** | Fix immediately (blocks demo) |
| **P2** | Fix before release/demo |
| **P3** | Fix when possible |

### Defect report minimum fields

Defect ID, Summary, Module, Environment, Preconditions, Steps, Expected, Actual, Severity, Priority, Attachments (screenshot/log), Status.

---

## 16. RISKS AND MITIGATION

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Neon / Render cold start | False “timeout” defects | High | Warm-up trước test; cron ping; tách perf vs functional |
| Email provider misconfig | Invitation / close-reg tests fail | High | Mock/log check + Brevo sandbox; verify `email_log` |
| RBL data inconsistency (seed duplicates) | Wrong expected ranks | Medium | Dùng `[QA-TEST]` deterministic; reconcile script chỉ khi cần |
| SQL Server vs PostgreSQL drift | Local pass / deploy fail | Medium | Primary test trên PostgreSQL/Neon; schema-postgresql làm chuẩn |
| Google OAuth/Forms secrets thiếu | Integration blocked | Medium | Mark conditional; skip nếu env thiếu; mock unit tests |
| Delayed feature delivery | Testing squeeze | Medium | Risk-based: Critical first; daily sync |
| Lack of UI automation | Regression cost cao | Medium | Maintain Critical manual checklist; expand unit tests |
| Concurrent registration race | Double booking seat | Low–Med | Test double-submit; rely on pessimistic lock in register |

---

## 17. TEST DELIVERABLES

| Deliverable | Description |
|---|---|
| **Test Plan** | Tài liệu này (`docs/TEST_PLAN_CampusEvent_ISTQB.md`) |
| **Test Cases** | Bộ case theo module (Excel/Sheets hoặc Markdown) |
| **Test Scripts** | Postman collection / SQL checks (optional) |
| **Defect Reports** | Issue tracker entries |
| **Test Execution Logs** | Pass/Fail matrix theo cycle |
| **Test Summary Report** | Kết quả cuối, metrics, residual risks |
| **RTM** | Requirement ↔ Test Case matrix |
| **Automation report** | `mvn test` / CI logs |

---

## 18. COMMUNICATION PLAN

| Activity | Frequency | Participants | Channel |
|---|---|---|---|
| Daily QA sync | Daily (or every standup) | QA + Dev | Meeting / Zalo / Discord |
| Defect review | 2× / week | QA + Dev | Meeting |
| Test status report | Weekly | Stakeholders / Mentor | Email / Docs |
| Demo readiness check | Before demo day | Full team | Meeting |
| Incident (prod/demo down) | Immediate | DevOps + Lead | Call / chat |

**Status report contents:** executed %, pass rate, open S1/S2, blockers, next 48h plan.

---

## 19. APPROVALS

| Name | Role | Signature | Date |
|---|---|---|---|
| | QA Lead | | |
| | Project Manager / Team Lead | | |
| | Mentor / Customer Representative | | |

---

# APPENDIX A — TEST CASE TEMPLATE

| Field | Description |
|---|---|
| Test Case ID | Unique ID, e.g. `TC-RBL-001` |
| Module | Functional area |
| Title | Short name |
| Priority | Critical / High / Medium / Low |
| Preconditions | Required state / data / login role |
| Test Steps | Numbered steps |
| Test Data | Inputs |
| Expected Result | Expected system behavior |
| Actual Result | Filled during execution |
| Status | Pass / Fail / Blocked / Not Run |
| Notes | Evidence link |

### Sample Test Cases (CampusEvent)

#### TC-AUTH-001 — Login Student thành công

| Field | Value |
|---|---|
| Module | Authentication |
| Priority | Critical |
| Preconditions | Seed student tồn tại, status active |
| Steps | 1) Mở `/api/login.html` 2) Nhập email/password hợp lệ 3) Submit |
| Expected | Đăng nhập thành công, chuyển portal Student, session lưu email/role |

#### TC-RBL-001 — Đăng ký vượt capacity → demote điểm thấp

| Field | Value |
|---|---|
| Module | RBL Registration |
| Priority | Critical |
| Preconditions | Event PUBLISHED, capacity = 3, đã có 3 REGISTERED; student mới điểm cao hơn người thấp nhất |
| Steps | 1) Login student mới 2) `POST /api/student/events/{id}/register` |
| Expected | Student mới = REGISTERED + có ticket; người điểm thấp nhất → WAITLIST + mất ticket |

#### TC-RBL-002 — Huỷ đăng ký → promote waitlist

| Field | Value |
|---|---|
| Module | RBL Registration |
| Priority | Critical |
| Preconditions | Event đầy, có ≥1 WAITLIST |
| Steps | 1) REGISTERED huỷ đăng ký 2) Quan sát waitlist đầu hàng |
| Expected | WAITLIST điểm cao nhất → REGISTERED + được issue ticket |

#### TC-ROOM-001 — Proposal bắt buộc chọn phòng từ danh mục

| Field | Value |
|---|---|
| Module | Rooms / Proposal |
| Priority | High |
| Preconditions | Có phòng active trong `/admin/rooms/options` |
| Steps | 1) Department mở form đề xuất 2) Không chọn phòng 3) Submit 4) Chọn phòng hợp lệ và submit lại |
| Expected | Bước 3 bị chặn; bước 4 tạo proposal với `roomId` + `location` = tên phòng |

#### TC-CLOSE-001 — Đóng đăng ký gửi email và chặn register mới

| Field | Value |
|---|---|
| Module | Close Registration |
| Priority | Critical |
| Preconditions | Event PUBLISHED, có REGISTERED; email đã cấu hình |
| Steps | 1) Admin Events → ⋯ → Đóng đăng ký 2) Confirm “Đóng sự kiện {tên}” 3) Student khác thử register |
| Expected | `registrationClosed=true`; email gửi REGISTERED; student mới nhận lỗi “Đăng ký sự kiện này đã đóng” |

#### TC-INV-001 — Scheduler chỉ gửi trong cửa sổ 7 ngày

| Field | Value |
|---|---|
| Module | Invitation |
| Priority | High |
| Preconditions | 2 event: A start trong 7 ngày, B start sau 10 ngày; cả hai có REGISTERED `invitationSentAt=null` |
| Steps | Chạy/đợi `InvitationScheduler` hoặc gọi logic tương đương |
| Expected | Chỉ A được gửi thư mời; B chưa gửi |

#### TC-EVT-001 — Auto-close sau endTime + 30 phút

| Field | Value |
|---|---|
| Module | Event Auto Close |
| Priority | High |
| Preconditions | Event PUBLISHED, `endTime` đã qua > 30 phút, `autoClosedAt` null |
| Steps | Đợi scheduler (hoặc chỉnh thời gian test) |
| Expected | Status `COMPLETED`, `autoClosedAt` set, REGISTERED không check-in → ABSENT |

---

# APPENDIX B — DEFECT REPORT TEMPLATE

| Field | Description |
|---|---|
| Defect ID | e.g. `DEF-042` |
| Summary | Short defect description |
| Module | Auth / RBL / Rooms / … |
| Environment | Local PG / Render+Neon |
| Preconditions | |
| Steps to Reproduce | |
| Expected Result | |
| Actual Result | |
| Severity | Critical / High / Medium / Low |
| Priority | P1 / P2 / P3 |
| Status | New / Assigned / Fixed / Retest / Closed |
| Attachments | Screenshot, API response, log excerpt |
| Found By / Date | |
| Fixed By / Date | |

---

# APPENDIX C — DECISION TABLE TESTING SAMPLE

### C1. Login

| Conditions | TC1 | TC2 | TC3 | TC4 |
|---|---|---|---|---|
| Valid Username/Email | Y | Y | N | N |
| Valid Password | Y | N | Y | N |
| **Expected Result** | Login Success | Invalid credentials | Invalid credentials | Login Failed |

### C2. RBL slot when event is full

| Conditions | TC1 | TC2 | TC3 | TC4 |
|---|---|---|---|---|
| Event has free seat | Y | N | N | N |
| Newcomer score > lowest REGISTERED | — | Y | N | Y |
| Registration closed | N | N | N | Y |
| **Expected Result** | New = REGISTERED | New = REGISTERED; lowest → WAITLIST | New = WAITLIST | Reject: closed |

### C3. Close registration email

| Conditions | TC1 | TC2 | TC3 |
|---|---|---|---|
| First time close | Y | N | Y |
| `resendEmails=true` | — | Y | — |
| Email provider OK | Y | Y | N |
| **Expected Result** | Close + send mails | Resend mails | Close OK, emailsFailed > 0 |

---

# APPENDIX D — USE CASE TESTING SAMPLE

### Use Case: Department tạo đề xuất sự kiện

| Step | User Action | Expected Result |
|---|---|---|
| 1 | Login role Department/Manager | Vào `screen-department.html` |
| 2 | Mở “Tạo đề xuất sự kiện” | Form hiện đủ section |
| 3 | Chọn khoa/chuyên ngành, giờ, **chọn phòng từ dropdown** | Phòng load từ `/admin/rooms/options` |
| 4 | Nhập mô tả, ngân sách, quiz (optional) | Dữ liệu được chấp nhận |
| 5 | Gửi đề xuất | Proposal status `PENDING`; hiện trong list khoa & hội đồng |

### Use Case: Committee duyệt đề xuất

| Step | User Action | Expected Result |
|---|---|---|
| 1 | Login Committee | `screen-committee.html` |
| 2 | Mở proposal PENDING | Thấy chi tiết + địa điểm |
| 3 | Bấm Duyệt, chọn phòng/time/capacity | Modal approve |
| 4 | Confirm | Event `PUBLISHED` tạo/reuse; proposal `APPROVED` |

### Use Case: Admin đóng sự kiện (đóng đăng ký)

| Step | User Action | Expected Result |
|---|---|---|
| 1 | Login Admin → Sự kiện | List events |
| 2 | ⋯ → Đóng đăng ký + gửi email | Dialog: **Đóng sự kiện {Tên}** |
| 3 | Xác nhận | ĐK đóng; badge “Đã đóng ĐK”; email gửi REGISTERED |
| 4 | Student thử đăng ký | Bị từ chối |

### Use Case: Student đăng ký theo RBL

| Step | User Action | Expected Result |
|---|---|---|
| 1 | Login Student | Portal student |
| 2 | Mở event PUBLISHED còn mở ĐK | Thấy điểm ưu tiên ước tính |
| 3 | Đăng ký | REGISTERED hoặc WAITLIST đúng rule RBL |
| 4 | Nếu REGISTERED | Có ticket; có thể nhận thư mời khi trong cửa sổ 7 ngày |

---

# APPENDIX E — REQUIREMENT TRACEABILITY MATRIX (RTM) — SAMPLE

| Req ID | Requirement | Module | Test Case IDs | Priority |
|---|---|---|---|---|
| REQ-01 | Người dùng đăng nhập theo vai trò | Auth | TC-AUTH-001… | Critical |
| REQ-02 | Khoa tạo proposal có địa điểm hợp lệ | Proposal/Rooms | TC-ROOM-001… | High |
| REQ-03 | Hội đồng duyệt tạo event | Committee | UC Approve | Critical |
| REQ-04 | Sinh viên đăng ký theo điểm RBL | RBL | TC-RBL-001, TC-RBL-002 | Critical |
| REQ-05 | Thư mời trước 7 ngày | Invitation | TC-INV-001 | High |
| REQ-06 | Admin đóng đăng ký + email | Close Reg | TC-CLOSE-001 | Critical |
| REQ-07 | Tự đóng event sau kết thúc + grace | Auto-close | TC-EVT-001 | High |
| REQ-08 | Check-in / điểm danh | Attendance | *(to expand)* | High |
| REQ-09 | Admin quản lý phòng CRUD | Rooms | TC-ROOM-* | High |
| REQ-10 | Deploy Render health | Ops | Smoke suite | High |

---

# APPENDIX F — CRITICAL REGRESSION CHECKLIST (DEMO DAY)

- [ ] `/api/auth/test` = OK  
- [ ] Login Admin / Department / Committee / Student  
- [ ] Tạo proposal + chọn phòng dropdown  
- [ ] Committee approve → event published  
- [ ] Student register (RBL) trên event capacity nhỏ  
- [ ] Cancel → waitlist promote  
- [ ] Admin đóng sự kiện → confirm text ngắn + email log  
- [ ] Manual “Gửi thư mời ngay” (optional demo)  
- [ ] Rooms page: search + pagination  
- [ ] Không seed phá DB prod nếu `APP_SEED_ENABLED` không chủ ý  

---

**END OF DOCUMENT**

*Document control: store under `docs/TEST_PLAN_CampusEvent_ISTQB.md`. Update version/history when scope changes (new module, new deploy target, or exit criteria change).*
