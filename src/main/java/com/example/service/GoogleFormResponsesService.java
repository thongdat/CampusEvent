package com.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đọc câu trả lời từ Google Form qua Forms API
 * GET https://forms.googleapis.com/v1/forms/{formId}/responses
 *
 * Trả về structured response: questionTitle → answer (text).
 * Không cần thêm scope vì forms.body đã có quyền đọc responses của form do user tạo.
 */
@Service
public class GoogleFormResponsesService {

    private static final Logger log = LoggerFactory.getLogger(GoogleFormResponsesService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FORMS_API = "https://forms.googleapis.com/v1/forms";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /** 1 lượt sinh viên submit form. */
    public static class FormResponse {
        public String responseId;
        public String respondentEmail;
        public Instant submittedAt;
        /** Map questionTitle (đã chuẩn hoá) → giá trị text. */
        public final Map<String, String> answers = new LinkedHashMap<>();

        public String get(String questionKey) {
            return answers.get(questionKey);
        }
    }

    /**
     * Lấy toàn bộ responses của 1 form (với cursor pagination).
     * @param sinceIso8601 ISO8601 (e.g. "2024-01-01T00:00:00Z") — chỉ lấy responses sau thời điểm này, null = lấy hết.
     */
    public List<FormResponse> listResponses(String formId, String accessToken, String sinceIso8601)
            throws GoogleFormsApiService.GoogleApiException {
        if (formId == null || formId.isBlank()) {
            throw new GoogleFormsApiService.GoogleApiException("Thiếu formId để gọi responses API.");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new GoogleFormsApiService.GoogleApiException("Thiếu access token Google.");
        }

        // Bước 1: lấy schema (itemId → title) để map answers
        Map<String, String> itemTitles = fetchItemTitles(formId, accessToken);

        // Bước 2: lấy responses với pagination
        List<FormResponse> all = new ArrayList<>();
        String pageToken = null;
        int safety = 0;
        do {
            StringBuilder url = new StringBuilder(FORMS_API + "/" + formId + "/responses?pageSize=200");
            if (sinceIso8601 != null && !sinceIso8601.isBlank()) {
                String filter = "timestamp > " + sinceIso8601;
                url.append("&filter=").append(URLEncoder.encode(filter, StandardCharsets.UTF_8));
            }
            if (pageToken != null) {
                url.append("&pageToken=").append(URLEncoder.encode(pageToken, StandardCharsets.UTF_8));
            }
            Map<String, Object> body = getJson(url.toString(), accessToken, "list responses");
            List<?> respList = (List<?>) body.get("responses");
            if (respList != null) {
                for (Object o : respList) {
                    if (o instanceof Map) {
                        all.add(parseResponse((Map<String, Object>) o, itemTitles));
                    }
                }
            }
            pageToken = (String) body.get("nextPageToken");
            safety++;
        } while (pageToken != null && safety < 20);
        return all;
    }

    /** Lấy bản đồ itemId → questionTitle để mapping. */
    @SuppressWarnings("unchecked")
    private Map<String, String> fetchItemTitles(String formId, String accessToken)
            throws GoogleFormsApiService.GoogleApiException {
        Map<String, String> idToTitle = new LinkedHashMap<>();
        Map<String, Object> form = getJson(FORMS_API + "/" + formId, accessToken, "get form schema");
        List<?> items = (List<?>) form.get("items");
        if (items == null) return idToTitle;
        for (Object item : items) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) item;
            String title = (String) m.getOrDefault("title", "");
            Object qi = m.get("questionItem");
            if (qi instanceof Map) {
                Object q = ((Map<String, Object>) qi).get("question");
                if (q instanceof Map) {
                    String questionId = (String) ((Map<String, Object>) q).get("questionId");
                    if (questionId != null) {
                        idToTitle.put(questionId, title.trim().toLowerCase());
                    }
                }
            }
        }
        return idToTitle;
    }

    @SuppressWarnings("unchecked")
    private FormResponse parseResponse(Map<String, Object> raw, Map<String, String> itemTitles) {
        FormResponse r = new FormResponse();
        r.responseId = (String) raw.get("responseId");
        r.respondentEmail = (String) raw.get("respondentEmail");
        String submitted = (String) raw.getOrDefault("lastSubmittedTime", raw.get("createTime"));
        try { r.submittedAt = submitted != null ? Instant.parse(submitted) : null; }
        catch (Exception ex) { r.submittedAt = null; }

        Object answersBlob = raw.get("answers");
        if (answersBlob instanceof Map) {
            ((Map<String, Object>) answersBlob).forEach((questionId, ans) -> {
                if (!(ans instanceof Map)) return;
                Map<String, Object> a = (Map<String, Object>) ans;
                String text = extractAnswerText(a);
                String title = itemTitles.get(questionId);
                if (title == null) title = questionId;
                r.answers.put(title, text);
            });
        }
        return r;
    }

    @SuppressWarnings("unchecked")
    private String extractAnswerText(Map<String, Object> answer) {
        Object textAnswers = answer.get("textAnswers");
        if (textAnswers instanceof Map) {
            Object list = ((Map<String, Object>) textAnswers).get("answers");
            if (list instanceof List) {
                StringBuilder sb = new StringBuilder();
                for (Object item : (List<?>) list) {
                    if (item instanceof Map) {
                        Object v = ((Map<String, Object>) item).get("value");
                        if (v != null) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(String.valueOf(v));
                        }
                    }
                }
                return sb.toString();
            }
        }
        return "";
    }

    // -- HTTP helper (tự gọi để không phụ thuộc package-private) --

    Map<String, Object> getJson(String url, String accessToken, String op)
            throws GoogleFormsApiService.GoogleApiException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();
            String body = resp.body();
            if (status >= 200 && status < 300) {
                if (body == null || body.isBlank()) return Map.of();
                return MAPPER.readValue(body, new TypeReference<Map<String, Object>>() {});
            }
            String human;
            if (status == 401) human = "Access token hết hạn. Đăng xuất → đăng nhập lại bằng Gmail.";
            else if (status == 403) human = "Bị từ chối (403). Forms API có thể chưa enable hoặc thiếu scope. Chi tiết: " + body;
            else if (status == 404) human = "Form không tồn tại hoặc bạn không phải owner. Chi tiết: " + body;
            else human = "HTTP " + status + " → " + body;
            throw new GoogleFormsApiService.GoogleApiException("Google API lỗi (" + op + "): " + human);
        } catch (GoogleFormsApiService.GoogleApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("HTTP error gọi {}: {}", op, ex.getMessage());
            throw new GoogleFormsApiService.GoogleApiException("Không gọi được Google API (" + op + "): " + ex.getMessage(), ex);
        }
    }
}
