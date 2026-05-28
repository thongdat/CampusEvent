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

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * Tạo form mới rồi điền câu hỏi.
     * @return URL responder (link gửi cho sinh viên scan QR)
     */
    public String createFormForEvent(Event event, String accessToken) throws GoogleApiException {
        if (accessToken == null || accessToken.isBlank()) {
            throw new GoogleApiException("Thiếu access token. Vui lòng đăng xuất rồi đăng nhập lại bằng Gmail "
                    + "để cấp quyền tạo Google Form.");
        }

        // Bước 1: tạo form rỗng (Forms API chỉ cho phép set title lúc create)
        Map<String, Object> createBody = Map.of(
                "info", Map.of("title", safeText(event.getTitle(), "Sự kiện FPT"))
        );
        Map<String, Object> created = postJson(FORMS_API, createBody, accessToken, "create form");
        String formId = (String) created.get("formId");
        String responderUri = (String) created.get("responderUri");
        if (formId == null) {
            throw new GoogleApiException("Phản hồi từ Google không có formId: " + created);
        }
        log.info("Đã tạo Google Form id={}, responderUri={}", formId, responderUri);

        // Bước 2: batchUpdate — đặt description, bật thu email, thêm ảnh + câu hỏi
        List<Map<String, Object>> requests = new ArrayList<>();

        // 2a) Description (subtitle dưới tiêu đề form)
        String desc = buildDescription(event);
        requests.add(Map.of(
                "updateFormInfo", Map.of(
                        "info", Map.of("description", desc),
                        "updateMask", "description"
                )
        ));

        // 2b) Bật thu thập email (Google sẽ tự thêm 1 trường email verified)
        requests.add(Map.of(
                "updateSettings", Map.of(
                        "settings", Map.of("emailCollectionType", "VERIFIED"),
                        "updateMask", "emailCollectionType"
                )
        ));

        int idx = 0;

        // 2c) Ảnh banner event (nếu có) — index 0
        if (event.getImageUrl() != null && !event.getImageUrl().isBlank()
                && event.getImageUrl().startsWith("http")) {
            requests.add(createImageItem(event.getImageUrl(),
                    "Ảnh sự kiện: " + safeText(event.getTitle(), "FPT Event"), idx++));
        }

        // 2d) Câu hỏi: Họ và tên
        requests.add(createTextItem("Họ và tên", "Vui lòng nhập đúng họ tên trên thẻ sinh viên", idx++));

        // 2e) Câu hỏi: MSSV
        requests.add(createTextItem("Mã số sinh viên (MSSV)", "VD: HE176543", idx++));

        // 2f) Câu hỏi: Giới tính (radio)
        requests.add(createChoiceItem("Giới tính", List.of("Nam", "Nữ", "Khác"), idx++));

        Map<String, Object> batchBody = Map.of("requests", requests);
        postJson(FORMS_API + "/" + formId + ":batchUpdate", batchBody, accessToken, "add questions");
        log.info("Đã thêm câu hỏi cho form {}", formId);

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

        // Trả URL responder (link sinh viên dùng)
        if (responderUri == null || responderUri.isBlank()) {
            responderUri = "https://docs.google.com/forms/d/e/" + formId + "/viewform";
        }
        return responderUri;
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

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    // =========== HTTP ===========

    private Map<String, Object> postJson(String url, Object body, String accessToken, String op)
            throws GoogleApiException {
        try {
            String json = MAPPER.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();
            String responseBody = resp.body();

            if (status >= 200 && status < 300) {
                if (responseBody == null || responseBody.isBlank()) return Map.of();
                return MAPPER.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            }

            String human = humanizeError(status, responseBody);
            throw new GoogleApiException("Google API lỗi (" + op + "): " + human);

        } catch (GoogleApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GoogleApiException("Không gọi được Google API: " + ex.getMessage(), ex);
        }
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
            if (lower.contains("insufficientpermissions") || lower.contains("insufficient_scope")) {
                return "Thiếu quyền OAuth. Đăng xuất → đăng nhập lại bằng Gmail và TICK đầy đủ các quyền Google yêu cầu.";
            }
            return "Bị từ chối (403). Chi tiết: " + body;
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
