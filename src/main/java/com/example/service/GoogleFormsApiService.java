package com.example.service;

import com.example.model.Event;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tạo Google Form tự động qua Google Forms API + share public qua Drive API.
 * Form sẽ có:
 *  - Tiêu đề = tên event
 *  - Mô tả  = thời gian + địa điểm + khoa
 *  - 4 câu hỏi bắt buộc: Email (verified) · Họ và tên · MSSV · Giới tính
 *
 * Yêu cầu: user đã đăng nhập Google với scope "forms.body" + "drive.file".
 * Forms API: https://forms.googleapis.com/v1/forms
 * Drive  API: https://www.googleapis.com/drive/v3
 */
@Service
public class GoogleFormsApiService {

    private static final Logger log = LoggerFactory.getLogger(GoogleFormsApiService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String FORMS_API = "https://forms.googleapis.com/v1/forms";
    private static final String DRIVE_API = "https://www.googleapis.com/drive/v3/files";
    private static final int MAX_WRITE_ATTEMPTS = 5;

    /** Google write requests are serialized to avoid project/user burst quotas. */
    private final Object formCreationGate = new Object();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /** Kết quả tạo form: URL cho sinh viên + formId (để gọi API responses) + sheetId. */
    public static class CreatedForm {
        public final String formId;
        public final String responderUri;
        public final String sheetId;
        public CreatedForm(String formId, String responderUri, String sheetId) {
            this.formId = formId;
            this.responderUri = responderUri;
            this.sheetId = sheetId;
        }
    }

    /** Câu hỏi quiz xác thực check-in (do Department định nghĩa) — DTO độc lập với entity. */
    public static class QuizItem {
        public final String text;
        public final List<String> options;
        public QuizItem(String text, List<String> options) {
            this.text = text;
            this.options = options;
        }
    }

    /**
     * Backward-compat: tạo CHECK-IN form đơn giản.
     * @return URL responder để render QR
     */
    public String createFormForEvent(Event event, String accessToken) throws GoogleApiException {
        return createCheckinForm(event, accessToken).responderUri;
    }

    /**
     * Tạo form CHECK-IN cho event — TỐI GIẢN để điểm danh nhanh:
     *  - Tiêu đề: [CHECK-IN] tên event
     *  - Mô tả: ảnh + thời gian + địa điểm + diễn giả
     *  - Câu hỏi: Email (Google tự thu thập & XÁC THỰC) + Họ tên + MSSV
     *  Việc tính check-in dựa trên email xác thực có khớp tài khoản hệ thống hay không.
     */
    public CreatedForm createCheckinForm(Event event, String accessToken) throws GoogleApiException {
        return createForm(event, accessToken, "CHECK-IN", false, List.of());
    }

    /**
     * Tạo form CHECK-OUT (quiz nhỏ + khảo sát feedback):
     *  - Tiêu đề: [CHECK-OUT] tên event
     *  - Câu hỏi: Email (xác thực) + MSSV + quiz nhỏ do Department định nghĩa
     *             + Rating (4 tiêu chí 1-5⭐) + 2 câu góp ý
     */
    public CreatedForm createCheckoutForm(Event event, String accessToken, List<QuizItem> quiz) throws GoogleApiException {
        return createForm(event, accessToken, "CHECK-OUT", true, quiz);
    }

    private CreatedForm createForm(Event event, String accessToken, String kind, boolean isCheckout, List<QuizItem> quiz)
            throws GoogleApiException {
        synchronized (formCreationGate) {
            return createFormSerial(event, accessToken, kind, isCheckout, quiz);
        }
    }

    private CreatedForm createFormSerial(Event event, String accessToken, String kind,
                                         boolean isCheckout, List<QuizItem> quiz)
            throws GoogleApiException {
        if (accessToken == null || accessToken.isBlank()) {
            throw new GoogleApiException("Thiếu access token. Vui lòng đăng xuất rồi đăng nhập lại bằng Gmail "
                    + "để cấp quyền tạo Google Form.");
        }

        // Bước 1: tạo form rỗng (Forms API chỉ cho phép set title lúc create)
        String formTitle = "[" + kind + "] " + safeText(event.getTitle(), "Sự kiện FPT");
        Map<String, Object> createBody = Map.of(
                "info", Map.of("title", formTitle)
        );
        Map<String, Object> created = postJson(FORMS_API, createBody, accessToken, "create form");
        String formId = (String) created.get("formId");
        String responderUri = (String) created.get("responderUri");
        if (formId == null) {
            throw new GoogleApiException("Phản hồi từ Google không có formId: " + created);
        }
        log.info("Đã tạo Google Form {} id={}, responderUri={}", kind, formId, responderUri);

        // Bước 2: batchUpdate — đặt description, bật thu email, thêm câu hỏi cốt lõi.
        // Ảnh được thêm ở request riêng vì một URL ảnh lỗi sẽ làm Google rollback cả batch.
        List<Map<String, Object>> requests = new ArrayList<>();

        String desc = isCheckout ? buildCheckoutDescription(event) : buildDescription(event);
        requests.add(Map.of(
                "updateFormInfo", Map.of(
                        "info", Map.of("description", desc),
                        "updateMask", "description"
                )
        ));

        requests.add(Map.of(
                "updateSettings", Map.of(
                        "settings", Map.of("emailCollectionType", "VERIFIED"),
                        "updateMask", "emailCollectionType"
                )
        ));

        int idx = 0;

        if (!isCheckout) {
            // === CHECK-IN form — tối giản: Họ tên + MSSV (email Google tự thu thập & xác thực) ===
            requests.add(createTextItem("Họ và tên", "Nhập đúng họ tên trên thẻ sinh viên", idx++));
            requests.add(createTextItem("Mã số sinh viên (MSSV)", "VD: HE176543", idx++));
        } else {
            // === CHECK-OUT form — MSSV + quiz nhỏ (Department) + feedback ===
            requests.add(createTextItem("Mã số sinh viên (MSSV)", "Khớp với MSSV bạn đã điền lúc check-in", idx++));

            // Quiz nhỏ do Department định nghĩa (đặt trước phần feedback)
            if (quiz != null) {
                int qNo = 1;
                for (QuizItem q : quiz) {
                    if (q == null || q.text == null || q.text.isBlank()) continue;
                    List<String> opts = new ArrayList<>();
                    if (q.options != null) {
                        for (String o : q.options) {
                            if (o != null && !o.isBlank()) opts.add(o.trim());
                        }
                    }
                    String title = "[Question " + qNo + "] " + q.text.trim();
                    if (opts.size() >= 2) {
                        requests.add(createChoiceItem(title, opts, idx++));
                    } else {
                        requests.add(createTextItem(title, "Trả lời ngắn gọn", idx++));
                    }
                    qNo++;
                }
            }

            // Feedback đánh giá buổi học
            requests.add(createChoiceItem("⭐ Nội dung sự kiện",
                    List.of("1 - Rất kém", "2 - Kém", "3 - Bình thường", "4 - Tốt", "5 - Xuất sắc"), idx++));
            requests.add(createChoiceItem("⭐ Diễn giả / chuyên gia",
                    List.of("1 - Rất kém", "2 - Kém", "3 - Bình thường", "4 - Tốt", "5 - Xuất sắc"), idx++));
            requests.add(createChoiceItem("⭐ Khâu tổ chức (hội trường, thời gian, tài liệu)",
                    List.of("1 - Rất kém", "2 - Kém", "3 - Bình thường", "4 - Tốt", "5 - Xuất sắc"), idx++));
            requests.add(createChoiceItem("⭐ Đánh giá tổng thể",
                    List.of("1 - Rất kém", "2 - Kém", "3 - Bình thường", "4 - Tốt", "5 - Xuất sắc"), idx++));

            requests.add(createParagraphItem("Điều bạn học được / ấn tượng nhất hôm nay?",
                    "Chia sẻ 2-3 câu — Ban tổ chức sẽ đọc tất cả phản hồi.", true, idx++));
            requests.add(createParagraphItem("Góp ý cải thiện cho lần tổ chức sau",
                    "Tuỳ chọn — bạn muốn lần sau có gì khác?", false, idx++));
        }

        Map<String, Object> batchBody = Map.of("requests", requests);
        postJson(FORMS_API + "/" + formId + ":batchUpdate", batchBody, accessToken, "add questions");
        log.info("Đã thêm câu hỏi cho form {} ({})", formId, kind);

        // Banner là phần trang trí, không được phép làm thất bại toàn bộ form.
        if (!isCheckout && event.getImageUrl() != null && !event.getImageUrl().isBlank()
                && event.getImageUrl().startsWith("http")) {
            try {
                Map<String, Object> imageBody = Map.of("requests", List.of(
                        createImageItem(event.getImageUrl(),
                                "Ảnh sự kiện: " + safeText(event.getTitle(), "FPT Event"), 0)));
                postJson(FORMS_API + "/" + formId + ":batchUpdate", imageBody, accessToken, "add banner");
            } catch (GoogleApiException ex) {
                log.warn("Bỏ qua banner lỗi của form {}: {}", formId, ex.getMessage());
            }
        }

        // Bước 3: share public (anyone with link → reader) qua Drive API
        try {
            Map<String, Object> perm = Map.of(
                    "type", "anyone",
                    "role", "reader"
            );
            postJson(DRIVE_API + "/" + formId + "/permissions?supportsAllDrives=true", perm, accessToken, "share form");
            log.info("Đã share form {} công khai", formId);
        } catch (GoogleApiException ex) {
            log.warn("Không share được form (vẫn dùng được nhưng user khác có thể không xem): {}", ex.getMessage());
        }

        // Bước 4: lấy linked sheetId (để background polling sau này)
        String sheetId = fetchLinkedSheetId(formId, accessToken);

        if (responderUri == null || responderUri.isBlank()) {
            responderUri = "https://docs.google.com/forms/d/e/" + formId + "/viewform";
        }
        return new CreatedForm(formId, responderUri, sheetId);
    }

    /**
     * Forms API GET form trả về `linkedSheetId` trong info.
     * Nếu form chưa link sheet (mặc định Google không tự tạo) → trả null.
     */
    private String fetchLinkedSheetId(String formId, String accessToken) {
        try {
            Map<String, Object> form = getJson(FORMS_API + "/" + formId, accessToken, "get form");
            Object linked = form.get("linkedSheetId");
            return linked == null ? null : String.valueOf(linked);
        } catch (Exception ex) {
            log.warn("Không lấy được linkedSheetId cho form {}: {}", formId, ex.getMessage());
            return null;
        }
    }

    // =========== helpers tạo question item ===========

    private Map<String, Object> createTextItem(String title, String desc, int index) {
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("required", true);
        question.put("textQuestion", Map.of("paragraph", false));

        Map<String, Object> questionItem = Map.of("question", question);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", title);
        if (desc != null && !desc.isBlank()) item.put("description", desc);
        item.put("questionItem", questionItem);

        return Map.of("createItem", Map.of(
                "item", item,
                "location", Map.of("index", index)
        ));
    }

    private Map<String, Object> createParagraphItem(String title, String desc, boolean required, int index) {
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("required", required);
        question.put("textQuestion", Map.of("paragraph", true));

        Map<String, Object> questionItem = Map.of("question", question);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", title);
        if (desc != null && !desc.isBlank()) item.put("description", desc);
        item.put("questionItem", questionItem);

        return Map.of("createItem", Map.of(
                "item", item,
                "location", Map.of("index", index)
        ));
    }

    private Map<String, Object> createImageItem(String imageUrl, String altText, int index) {
        Map<String, Object> image = Map.of(
                "sourceUri", imageUrl,
                "altText", altText == null ? "" : altText
        );
        Map<String, Object> imageItem = Map.of("image", image);
        Map<String, Object> item = Map.of(
                "title", "📌 Sự kiện FPT",
                "imageItem", imageItem
        );
        return Map.of("createItem", Map.of(
                "item", item,
                "location", Map.of("index", index)
        ));
    }

    private Map<String, Object> createChoiceItem(String title, List<String> options, int index) {
        List<Map<String, Object>> optList = new ArrayList<>();
        for (String opt : options) optList.add(Map.of("value", opt));

        Map<String, Object> choiceQuestion = Map.of(
                "type", "RADIO",
                "options", optList,
                "shuffle", false
        );
        Map<String, Object> question = Map.of(
                "required", true,
                "choiceQuestion", choiceQuestion
        );
        Map<String, Object> questionItem = Map.of("question", question);
        Map<String, Object> item = Map.of(
                "title", title,
                "questionItem", questionItem
        );
        return Map.of("createItem", Map.of(
                "item", item,
                "location", Map.of("index", index)
        ));
    }

    private String buildDescription(Event event) {
        StringBuilder sb = new StringBuilder();
        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            sb.append(event.getDescription()).append("\n\n");
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        if (event.getStartTime() != null) {
            sb.append("📅 THỜI GIAN: ")
              .append(formatDateTime(event.getStartTime()));
            if (event.getEndTime() != null) sb.append(" → ").append(formatDateTime(event.getEndTime()));
            sb.append("\n");
        }
        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            sb.append("📍 ĐỊA ĐIỂM: ").append(event.getLocation()).append("\n");
        }
        if (event.getDepartment() != null && event.getDepartment().getName() != null) {
            sb.append("🏛 ĐƠN VỊ TỔ CHỨC: ").append(event.getDepartment().getName()).append("\n");
        }
        if (event.getCapacity() != null && event.getCapacity() > 0) {
            sb.append("👥 SỨC CHỨA: ").append(event.getCapacity()).append(" sinh viên\n");
        }
        if (event.getSpeakers() != null && !event.getSpeakers().isBlank()) {
            sb.append("\n🎤 DIỄN GIẢ / CHUYÊN GIA:\n").append(event.getSpeakers()).append("\n");
        }
        if (event.getImageUrl() != null && !event.getImageUrl().isBlank()) {
            sb.append("\n🖼 Hình ảnh sự kiện: ").append(event.getImageUrl()).append("\n");
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("👉 Vui lòng điền đầy đủ thông tin bên dưới để hoàn tất check-in.\n");
        sb.append("Form tự động sinh bởi FPT Campus Events · AEMS Toolkit.");
        return sb.toString();
    }

    private String formatDateTime(java.time.LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy"));
    }

    private String buildCheckoutDescription(Event event) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎓 Cảm ơn bạn đã tham gia ");
        sb.append(safeText(event.getTitle(), "sự kiện FPT")).append("!\n\n");
        sb.append("Hãy dành 1-2 phút phản hồi để Ban tổ chức cải thiện các buổi sau.\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        if (event.getStartTime() != null) {
            sb.append("📅 Thời gian: ").append(formatDateTime(event.getStartTime()));
            if (event.getEndTime() != null) sb.append(" → ").append(formatDateTime(event.getEndTime()));
            sb.append("\n");
        }
        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            sb.append("📍 Địa điểm: ").append(event.getLocation()).append("\n");
        }
        if (event.getDepartment() != null && event.getDepartment().getName() != null) {
            sb.append("🏛 Đơn vị tổ chức: ").append(event.getDepartment().getName()).append("\n");
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("👉 Lưu ý: MSSV phải khớp với MSSV bạn đã điền lúc check-in để được tính là hoàn thành đủ buổi.");
        return sb.toString();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    // =========== HTTP ===========

    /** GET JSON → trả Map; dùng để đọc thông tin form (lấy linkedSheetId, ...). */
    Map<String, Object> getJson(String url, String accessToken, String op) throws GoogleApiException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();
            String responseBody = resp.body();
            if (status >= 200 && status < 300) {
                if (responseBody == null || responseBody.isBlank()) return Map.of();
                return MAPPER.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            }
            throw new GoogleApiException("Google API lỗi (" + op + "): " + humanizeError(status, responseBody));
        } catch (GoogleApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GoogleApiException("Không gọi được Google API (" + op + "): " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> postJson(String url, Object body, String accessToken, String op)
            throws GoogleApiException {
        try {
            String json = MAPPER.writeValueAsString(body);
            for (int attempt = 1; attempt <= MAX_WRITE_ATTEMPTS; attempt++) {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Authorization", "Bearer " + accessToken)
                            .header("Content-Type", "application/json; charset=UTF-8")
                            .timeout(Duration.ofSeconds(25))
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();
                    HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
                    int status = resp.statusCode();
                    String responseBody = resp.body();

                    if (status >= 200 && status < 300) {
                        if (responseBody == null || responseBody.isBlank()) return Map.of();
                        return MAPPER.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                    }
                    if (isRetryable(status) && attempt < MAX_WRITE_ATTEMPTS) {
                        waitBeforeRetry(op, attempt, retryAfterMillis(resp));
                        continue;
                    }
                    throw new GoogleApiException("Google API lỗi (" + op + "): "
                            + humanizeError(status, responseBody));
                } catch (java.io.IOException ex) {
                    if (attempt >= MAX_WRITE_ATTEMPTS) {
                        throw new GoogleApiException("Không gọi được Google API (" + op + "): " + ex.getMessage(), ex);
                    }
                    waitBeforeRetry(op, attempt, 0);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new GoogleApiException("Tác vụ Google API đã bị gián đoạn.", ex);
                }
            }
            throw new GoogleApiException("Google API không phản hồi sau nhiều lần thử (" + op + ").");
        } catch (GoogleApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GoogleApiException("Không gọi được Google API: " + ex.getMessage(), ex);
        }
    }

    static boolean isRetryable(int status) {
        return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private long retryAfterMillis(HttpResponse<?> response) {
        return response.headers().firstValue("Retry-After")
                .map(value -> {
                    try {
                        return Math.min(15_000L, Long.parseLong(value.trim()) * 1_000L);
                    } catch (NumberFormatException ignored) {
                        return 0L;
                    }
                })
                .orElse(0L);
    }

    private void waitBeforeRetry(String op, int attempt, long retryAfterMillis)
            throws InterruptedException {
        long exponential = Math.min(8_000L, 500L * (1L << Math.min(attempt - 1, 4)));
        long delay = Math.max(exponential, retryAfterMillis);
        log.warn("Google API tạm lỗi khi {}. Thử lại lần {} sau {} ms", op, attempt + 1, delay);
        Thread.sleep(delay);
    }

    private String humanizeError(int status, String body) {
        if (status == 401) {
            return "Access token hết hạn hoặc không hợp lệ. Vui lòng đăng xuất và đăng nhập lại bằng Gmail.";
        }
        if (status == 403) {
            String lower = body == null ? "" : body.toLowerCase();
            if (lower.contains("forms.googleapis.com") || lower.contains("api has not been used")) {
                return "Google Forms API chưa được bật. Vào https://console.cloud.google.com/apis/library/forms.googleapis.com → bấm ENABLE.";
            }
            if (lower.contains("drive.googleapis.com")) {
                return "Google Drive API chưa được bật. Vào https://console.cloud.google.com/apis/library/drive.googleapis.com → bấm ENABLE.";
            }
            if (lower.contains("insufficientpermissions") || lower.contains("insufficient_scope")
                    || lower.contains("access_token_scope_insufficient")
                    || lower.contains("insufficient authentication scopes")) {
                return "Thiếu quyền OAuth. Đăng xuất → đăng nhập lại bằng Gmail và TICK đầy đủ các quyền Google yêu cầu.";
            }
            return "Bị từ chối (403). Chi tiết: " + body;
        }
        if (status == 429) {
            return "Google đang giới hạn tốc độ tạo form. Hệ thống đã tự thử lại nhiều lần; vui lòng đợi một phút rồi thử lại.";
        }
        if (status == 404) {
            return "Endpoint Google không tồn tại (có thể project chưa enable API).";
        }
        return "HTTP " + status + " → " + body;
    }

    public static class GoogleApiException extends Exception {
        public GoogleApiException(String message) { super(message); }
        public GoogleApiException(String message, Throwable cause) { super(message, cause); }
    }
}
