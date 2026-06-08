package com.example.service;

import com.example.model.Event;
import com.example.model.EventFeedback;
import com.example.repository.EventFeedbackRepository;
import com.example.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Phân tích sự kiện học thuật dựa trên phản hồi (rating + bình luận) của sinh viên.
 *
 * Engine nội bộ (luôn hoạt động, không cần API key):
 *  - Phân tích cảm xúc (sentiment) tiếng Việt theo từ điển + xử lý phủ định ("không").
 *  - Trích xuất chủ đề theo khía cạnh (nội dung, diễn giả, tổ chức, thời gian, cơ sở vật chất...).
 *  - Tổng hợp điểm mạnh / điểm cần cải thiện và khuyến nghị hành động.
 *  - Sinh bản tóm tắt ngôn ngữ tự nhiên.
 *
 * Nâng cấp tuỳ chọn: nếu cấu hình GEMINI_API_KEY, phần tóm tắt sẽ được tạo bởi
 * Google Gemini cho văn phong tự nhiên hơn; lỗi/thiếu key sẽ tự fallback về engine nội bộ.
 */
@Service
public class FeedbackAiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackAiAnalysisService.class);

    private static final List<String> POSITIVE_WORDS = Arrays.asList(
            "tốt", "hay", "tuyệt", "tuyệt vời", "xuất sắc", "bổ ích", "hữu ích", "ý nghĩa",
            "nhiệt tình", "chuyên nghiệp", "hấp dẫn", "thú vị", "rõ ràng", "ấn tượng",
            "hài lòng", "thích", "đáng", "vui", "sôi nổi", "hào hứng", "chất lượng",
            "tâm huyết", "dễ hiểu", "phong phú", "chu đáo", "đúng giờ", "thân thiện", "truyền cảm hứng");

    private static final List<String> NEGATIVE_WORDS = Arrays.asList(
            "chán", "tệ", "dở", "kém", "lộn xộn", "ồn", "ồn ào", "khó hiểu", "thất vọng",
            "nhàm", "nhàm chán", "dài dòng", "thiếu", "chậm", "lê thê", "đông", "nóng",
            "lạnh", "trễ", "muộn", "rối", "qua loa", "sơ sài", "hời hợt", "buồn ngủ",
            "mệt", "khô khan", "lan man", "chật", "yếu", "nghèo nàn");

    private static final List<String> NEGATIONS = Arrays.asList("không", "chẳng", "chưa", "kém phần", "thiếu");

    /** Từ điển khía cạnh: nhãn -> các từ khoá nhận diện. */
    private static final Map<String, List<String>> ASPECTS = new LinkedHashMap<>();
    static {
        ASPECTS.put("Nội dung", Arrays.asList("nội dung", "kiến thức", "bài giảng", "chủ đề", "thông tin", "học được", "tài liệu"));
        ASPECTS.put("Diễn giả", Arrays.asList("diễn giả", "speaker", "giảng viên", "thầy", "cô", "trình bày", "chia sẻ", "nói chuyện"));
        ASPECTS.put("Khâu tổ chức", Arrays.asList("tổ chức", "sắp xếp", "chương trình", "kịch bản", "mc", "dẫn chương trình", "quy trình", "check-in", "checkin", "đăng ký"));
        ASPECTS.put("Thời gian", Arrays.asList("thời gian", "thời lượng", "giờ", "dài", "ngắn", "lâu", "kéo dài", "đúng giờ"));
        ASPECTS.put("Cơ sở vật chất", Arrays.asList("địa điểm", "phòng", "hội trường", "chỗ ngồi", "ghế", "không gian", "sân khấu", "máy chiếu", "slide"));
        ASPECTS.put("Âm thanh & ánh sáng", Arrays.asList("âm thanh", "loa", "micro", "mic", "ánh sáng", "nghe"));
        ASPECTS.put("Phúc lợi & quà tặng", Arrays.asList("quà", "gift", "đồ ăn", "nước", "ăn uống", "voucher", "phần thưởng"));
        ASPECTS.put("Tương tác & không khí", Arrays.asList("tương tác", "giao lưu", "hỏi đáp", "không khí", "trò chơi", "minigame", "giải lao"));
    }

    private final EventFeedbackRepository feedbackRepository;
    private final EventRepository eventRepository;

    @Value("${ai.gemini.api-key:${GEMINI_API_KEY:}}")
    private String geminiApiKey;

    public FeedbackAiAnalysisService(EventFeedbackRepository feedbackRepository, EventRepository eventRepository) {
        this.feedbackRepository = feedbackRepository;
        this.eventRepository = eventRepository;
    }

    public Map<String, Object> analyze(Long eventId) {
        Map<String, Object> result = new LinkedHashMap<>();
        Event event = eventRepository.findById(eventId).orElse(null);
        String title = event != null ? event.getTitle() : ("Sự kiện #" + eventId);
        result.put("eventTitle", title);

        List<EventFeedback> feedbacks = feedbackRepository.findByEventId(eventId);
        result.put("feedbackCount", feedbacks.size());
        if (feedbacks.isEmpty()) {
            result.put("available", false);
            result.put("source", "Phân tích nội bộ AEMS");
            result.put("summary", "Chưa có phản hồi nào để phân tích. Hãy khuyến khích sinh viên hoàn tất khảo sát check-out.");
            return result;
        }
        result.put("available", true);

        double avgContent = avg(feedbacks, EventFeedback::getContentRating);
        double avgSpeaker = avg(feedbacks, EventFeedback::getSpeakerRating);
        double avgOrg = avg(feedbacks, EventFeedback::getOrganizationRating);
        double avgOverall = avg(feedbacks, EventFeedback::getOverallRating);

        Map<String, Object> ratings = new LinkedHashMap<>();
        ratings.put("content", avgContent);
        ratings.put("speaker", avgSpeaker);
        ratings.put("organization", avgOrg);
        ratings.put("overall", avgOverall);
        result.put("ratings", ratings);

        int[] dist = new int[6];
        for (EventFeedback f : feedbacks) {
            int r = f.getOverallRating() == null ? 0 : Math.max(1, Math.min(5, f.getOverallRating()));
            if (r >= 1) dist[r]++;
        }
        List<Map<String, Object>> distribution = new ArrayList<>();
        for (int star = 5; star >= 1; star--) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("star", star);
            row.put("count", dist[star]);
            row.put("percent", percent(dist[star], feedbacks.size()));
            distribution.add(row);
        }
        result.put("distribution", distribution);

        // Sentiment + khía cạnh
        int pos = 0, neu = 0, neg = 0;
        Map<String, int[]> aspectScores = new LinkedHashMap<>();
        for (String aspect : ASPECTS.keySet()) {
            aspectScores.put(aspect, new int[]{0, 0});
        }
        List<String> positiveComments = new ArrayList<>();
        List<String> concernComments = new ArrayList<>();

        for (EventFeedback f : feedbacks) {
            String comment = f.getComment() == null ? "" : f.getComment().trim();
            int sentiment = blendSentiment(comment, f.getOverallRating());
            if (sentiment > 0) pos++;
            else if (sentiment < 0) neg++;
            else neu++;

            if (!comment.isBlank()) {
                if (sentiment > 0 && positiveComments.size() < 3) {
                    positiveComments.add(comment);
                } else if (sentiment < 0 && concernComments.size() < 3) {
                    concernComments.add(comment);
                }
                String lower = comment.toLowerCase(Locale.ROOT);
                for (Map.Entry<String, List<String>> e : ASPECTS.entrySet()) {
                    if (containsAny(lower, e.getValue())) {
                        int[] sc = aspectScores.get(e.getKey());
                        if (sentiment >= 0) sc[0]++;
                        else sc[1]++;
                    }
                }
            }
        }

        int total = feedbacks.size();
        Map<String, Object> sentiment = new LinkedHashMap<>();
        sentiment.put("positive", pos);
        sentiment.put("neutral", neu);
        sentiment.put("negative", neg);
        sentiment.put("positiveRate", percent(pos, total));
        sentiment.put("negativeRate", percent(neg, total));
        result.put("sentiment", sentiment);

        // Chủ đề nổi bật
        List<Map<String, Object>> themes = new ArrayList<>();
        List<String> strengths = new ArrayList<>();
        List<String> concerns = new ArrayList<>();
        aspectScores.entrySet().stream()
                .filter(e -> (e.getValue()[0] + e.getValue()[1]) > 0)
                .sorted((a, b) -> Integer.compare(b.getValue()[0] + b.getValue()[1], a.getValue()[0] + a.getValue()[1]))
                .forEach(e -> {
                    int p = e.getValue()[0];
                    int n = e.getValue()[1];
                    String tone = n > p ? "negative" : (p > 0 ? "positive" : "neutral");
                    Map<String, Object> theme = new LinkedHashMap<>();
                    theme.put("label", e.getKey());
                    theme.put("count", p + n);
                    theme.put("positive", p);
                    theme.put("negative", n);
                    theme.put("tone", tone);
                    themes.add(theme);
                    if (n > p) concerns.add(e.getKey());
                    else if (p > 0 && n == 0) strengths.add(e.getKey());
                });
        result.put("themes", themes);

        // Khuyến nghị hành động
        result.put("recommendations", buildRecommendations(avgContent, avgSpeaker, avgOrg, avgOverall, concerns, (double) neg / total));
        result.put("highlights", positiveComments);
        result.put("concernComments", concernComments);

        // Tóm tắt: ưu tiên Gemini nếu có key, fallback engine nội bộ
        String localSummary = buildLocalSummary(title, total, avgOverall, pos, neg, strengths, concerns);
        String summary = tryGeminiSummary(title, total, avgOverall, ratings, sentiment, themes, feedbacks);
        if (summary != null && !summary.isBlank()) {
            result.put("summary", summary);
            result.put("source", "Google Gemini");
        } else {
            result.put("summary", localSummary);
            result.put("source", "Phân tích nội bộ AEMS");
        }
        return result;
    }

    private List<String> buildRecommendations(double content, double speaker, double org, double overall,
                                              List<String> concerns, double negRate) {
        List<String> recs = new ArrayList<>();
        Map<String, Double> dims = new LinkedHashMap<>();
        dims.put("nội dung chương trình", content);
        dims.put("phần trình bày của diễn giả", speaker);
        dims.put("khâu tổ chức", org);
        String lowestKey = null;
        double lowest = Double.MAX_VALUE;
        for (Map.Entry<String, Double> e : dims.entrySet()) {
            if (e.getValue() < lowest) {
                lowest = e.getValue();
                lowestKey = e.getKey();
            }
        }
        if (lowestKey != null && lowest < 4.0) {
            recs.add(String.format(Locale.US,
                    "Ưu tiên cải thiện %s — đây là tiêu chí bị đánh giá thấp nhất (%.1f/5).", lowestKey, lowest));
        }
        if (overall < 3.5) {
            recs.add("Mức hài lòng chung đang thấp: nên họp rút kinh nghiệm và khảo sát sâu nguyên nhân trước kỳ tổ chức tới.");
        } else if (overall >= 4.5) {
            recs.add("Sự kiện được đánh giá rất cao — nên chuẩn hoá quy trình này thành mẫu cho các sự kiện sau.");
        }
        for (String concern : concerns) {
            recs.add("Xem lại khía cạnh \"" + concern + "\" vì nhận nhiều phản hồi tiêu cực.");
        }
        if (negRate >= 0.3) {
            recs.add("Tỉ lệ phản hồi tiêu cực cao (≥30%) — cân nhắc khảo sát bổ sung để làm rõ kỳ vọng của sinh viên.");
        }
        if (recs.isEmpty()) {
            recs.add("Duy trì chất lượng hiện tại; có thể thử nghiệm thêm hoạt động tương tác để nâng trải nghiệm.");
        }
        return recs;
    }

    private String buildLocalSummary(String title, int total, double overall, int pos, int neg,
                                     List<String> strengths, List<String> concerns) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sự kiện \"").append(title).append("\" nhận ").append(total)
          .append(" phản hồi với điểm tổng thể trung bình ").append(String.format(Locale.US, "%.1f", overall)).append("/5. ");
        int posRate = percent(pos, total);
        if (posRate >= 60) {
            sb.append("Phần lớn sinh viên có cảm nhận tích cực (").append(posRate).append("%). ");
        } else if (neg > pos) {
            sb.append("Cảm nhận tiêu cực đang chiếm ưu thế, cần lưu ý. ");
        } else {
            sb.append("Cảm nhận của sinh viên ở mức trung lập. ");
        }
        if (!strengths.isEmpty()) {
            sb.append("Điểm mạnh nổi bật: ").append(String.join(", ", strengths)).append(". ");
        }
        if (!concerns.isEmpty()) {
            sb.append("Cần cải thiện: ").append(String.join(", ", concerns)).append(".");
        }
        return sb.toString().trim();
    }

    /** Gọi Google Gemini để sinh tóm tắt; trả về null nếu không cấu hình hoặc lỗi. */
    private String tryGeminiSummary(String title, int total, double overall,
                                    Map<String, Object> ratings, Map<String, Object> sentiment,
                                    List<Map<String, Object>> themes, List<EventFeedback> feedbacks) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return null;
        }
        try {
            StringBuilder comments = new StringBuilder();
            int n = 0;
            for (EventFeedback f : feedbacks) {
                if (f.getComment() != null && !f.getComment().isBlank()) {
                    comments.append("- ").append(f.getComment().trim().replace("\n", " ")).append("\n");
                    if (++n >= 25) break;
                }
            }
            String prompt = "Bạn là trợ lý phân tích phản hồi sự kiện học thuật của trường đại học. "
                    + "Hãy viết một đoạn tóm tắt ngắn (3-5 câu) bằng tiếng Việt, khách quan, nêu rõ mức độ hài lòng, "
                    + "điểm mạnh và điểm cần cải thiện.\n\n"
                    + "Sự kiện: " + title + "\n"
                    + "Số phản hồi: " + total + "\n"
                    + "Điểm trung bình (1-5): tổng thể " + overall + ", nội dung " + ratings.get("content")
                    + ", diễn giả " + ratings.get("speaker") + ", tổ chức " + ratings.get("organization") + "\n"
                    + "Cảm xúc: tích cực " + sentiment.get("positive") + ", trung lập " + sentiment.get("neutral")
                    + ", tiêu cực " + sentiment.get("negative") + "\n"
                    + "Bình luận tiêu biểu:\n" + comments;

            String body = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]}]}";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, java.nio.charset.StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.warn("Gemini trả về HTTP {} - dùng tóm tắt nội bộ.", response.statusCode());
                return null;
            }
            return extractGeminiText(response.body());
        } catch (Exception ex) {
            log.warn("Không gọi được Gemini ({}). Dùng tóm tắt nội bộ.", ex.getMessage());
            return null;
        }
    }

    /** Rút phần text đầu tiên từ JSON Gemini mà không cần thư viện ngoài. */
    private String extractGeminiText(String json) {
        if (json == null) return null;
        int idx = json.indexOf("\"text\"");
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                switch (c) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        String text = sb.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    /** Kết hợp sentiment của bình luận với điểm overall để phân loại 1 phản hồi. */
    private int blendSentiment(String comment, Integer overall) {
        int textScore = textSentiment(comment);
        int ratingScore = 0;
        if (overall != null) {
            if (overall >= 4) ratingScore = 1;
            else if (overall <= 2) ratingScore = -1;
        }
        int sum = textScore + ratingScore;
        if (sum > 0) return 1;
        if (sum < 0) return -1;
        return 0;
    }

    private int textSentiment(String comment) {
        if (comment == null || comment.isBlank()) return 0;
        String lower = comment.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String word : POSITIVE_WORDS) {
            int from = 0;
            int at;
            while ((at = lower.indexOf(word, from)) >= 0) {
                score += isNegated(lower, at) ? -1 : 1;
                from = at + word.length();
            }
        }
        for (String word : NEGATIVE_WORDS) {
            int from = 0;
            int at;
            while ((at = lower.indexOf(word, from)) >= 0) {
                score += isNegated(lower, at) ? 1 : -1;
                from = at + word.length();
            }
        }
        return Integer.compare(score, 0);
    }

    /** Có từ phủ định ngay trước (trong 12 ký tự) không? */
    private boolean isNegated(String text, int pos) {
        int start = Math.max(0, pos - 12);
        String window = text.substring(start, pos);
        for (String neg : NEGATIONS) {
            if (window.contains(neg)) return true;
        }
        return false;
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    private double avg(List<EventFeedback> feedbacks, java.util.function.Function<EventFeedback, Integer> mapper) {
        return Math.round(feedbacks.stream()
                .map(mapper)
                .filter(v -> v != null && v > 0)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0) * 10.0) / 10.0;
    }

    private int percent(int part, int total) {
        return total == 0 ? 0 : (int) Math.round(part * 100.0 / total);
    }
}
