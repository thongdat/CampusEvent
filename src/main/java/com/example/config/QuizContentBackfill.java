package com.example.config;

import com.example.model.Event;
import com.example.model.EventProposal;
import com.example.model.QuizQuestion;
import com.example.repository.EventProposalRepository;
import com.example.repository.EventRepository;
import com.example.repository.QuizQuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Backfill quiz mẫu cho mọi event đã có sẵn (past + current + upcoming).
 * Mục tiêu: ngay sau khi start app, mọi event đều có 4 câu hỏi → QR check-in chạy được luôn,
 * Department không cần tự tạo quiz để test luồng.
 *
 * Chiến lược:
 *  - Mỗi event lấy 4 câu: 2 câu chuyên môn (theo chủ đề suy ra từ title/department)
 *    + 2 câu FPT chung. Shuffle deterministic theo eventId để stable giữa các lần restart.
 *  - Đồng thời sync sang EventProposal.quizPayload (JSON) để Department mở proposal sẽ thấy quiz đã có.
 *  - Idempotent: nếu event đã có ≥1 quiz hoặc proposal đã có quizPayload → bỏ qua.
 */
@Component
@Order(70) // chạy sau PastEventDataBackfill
public class QuizContentBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QuizContentBackfill.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final EventRepository eventRepository;
    private final EventProposalRepository proposalRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    public QuizContentBackfill(EventRepository eventRepository,
                               EventProposalRepository proposalRepository,
                               QuizQuestionRepository quizQuestionRepository) {
        this.eventRepository = eventRepository;
        this.proposalRepository = proposalRepository;
        this.quizQuestionRepository = quizQuestionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            int evCreated = backfillEvents();
            int prCreated = backfillProposals();
            if (evCreated + prCreated > 0) {
                log.info("QuizContentBackfill: đã tạo quiz cho {} event và sync {} proposal", evCreated, prCreated);
            }
        } catch (Exception ex) {
            log.warn("QuizContentBackfill bỏ qua do lỗi: {}", ex.getMessage());
        }
    }

    // ============== Event ==============

    private int backfillEvents() {
        int touched = 0;
        for (Event event : eventRepository.findAll()) {
            if (quizQuestionRepository.countByEventId(event.getId()) > 0) {
                continue; // đã có quiz, không ghi đè
            }
            List<QuestionSpec> picked = pickQuestions(event);
            for (QuestionSpec spec : picked) {
                QuizQuestion q = new QuizQuestion();
                q.setEvent(event);
                q.setQuestionText(spec.text);
                q.setQuestionType("MULTIPLE_CHOICE");
                q.setOptionA(spec.options[0]);
                q.setOptionB(spec.options[1]);
                q.setOptionC(spec.options[2]);
                q.setOptionD(spec.options[3]);
                q.setCorrectAnswer(spec.correct);
                q.setPoints(spec.points);
                quizQuestionRepository.save(q);
            }
            touched++;
        }
        return touched;
    }

    // ============== EventProposal — sync quizPayload JSON ==============

    private int backfillProposals() {
        int touched = 0;
        for (EventProposal proposal : proposalRepository.findAll()) {
            if (proposal.getQuizPayload() != null && !proposal.getQuizPayload().isBlank()) {
                continue;
            }
            List<QuestionSpec> picked = pickQuestions(proposal.getTitle(), proposal.getDescription(),
                    proposal.getDepartment() != null ? proposal.getDepartment().getName() : "",
                    proposal.getId());
            List<Map<String, Object>> payload = new ArrayList<>();
            for (QuestionSpec spec : picked) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("questionText", spec.text);
                m.put("questionType", "MULTIPLE_CHOICE");
                m.put("optionA", spec.options[0]);
                m.put("optionB", spec.options[1]);
                m.put("optionC", spec.options[2]);
                m.put("optionD", spec.options[3]);
                m.put("correctAnswer", spec.correct);
                m.put("points", spec.points);
                payload.add(m);
            }
            try {
                proposal.setQuizPayload(MAPPER.writeValueAsString(payload));
                proposalRepository.save(proposal);
                touched++;
            } catch (Exception ignored) {}
        }
        return touched;
    }

    // ============== Question picker ==============

    private List<QuestionSpec> pickQuestions(Event event) {
        return pickQuestions(event.getTitle(),
                event.getDescription(),
                event.getDepartment() != null ? event.getDepartment().getName() : "",
                event.getId());
    }

    private List<QuestionSpec> pickQuestions(String title, String description, String departmentName, Long seed) {
        String topic = detectTopic(title, description, departmentName);
        List<QuestionSpec> domain = QUESTION_POOLS.getOrDefault(topic, QUESTION_POOLS.get("default"));
        List<QuestionSpec> general = QUESTION_POOLS.get("fpt-general");

        Random rng = new Random(seed == null ? 1L : seed);
        List<QuestionSpec> pickedDomain = pickN(domain, 2, rng);
        List<QuestionSpec> pickedGeneral = pickN(general, 2, rng);
        List<QuestionSpec> result = new ArrayList<>();
        result.addAll(pickedDomain);
        result.addAll(pickedGeneral);
        return result;
    }

    private List<QuestionSpec> pickN(List<QuestionSpec> source, int n, Random rng) {
        List<QuestionSpec> copy = new ArrayList<>(source);
        Collections.shuffle(copy, rng);
        return copy.subList(0, Math.min(n, copy.size()));
    }

    /** Suy ra chủ đề từ title + description + tên department. */
    private String detectTopic(String title, String description, String departmentName) {
        String hay = ((title == null ? "" : title) + " "
                + (description == null ? "" : description) + " "
                + (departmentName == null ? "" : departmentName))
                .toLowerCase(Locale.ROOT);
        if (containsAny(hay, "ai", "machine learning", "ml", "trí tuệ nhân tạo", "data science", "deep learning", "llm", "gpt", "neural")) return "ai";
        if (containsAny(hay, "cloud", "aws", "azure", "gcp", "kubernetes", "docker", "devops")) return "cloud";
        if (containsAny(hay, "mobile", "android", "ios", "flutter", "react native", "swift", "kotlin")) return "mobile";
        if (containsAny(hay, "frontend", "react", "vue", "angular", "tailwind", "ui/ux", "web app", "html", "css")) return "web";
        if (containsAny(hay, "backend", "spring", "java", ".net", "node.js", "api", "microservice", "database", "sql")) return "backend";
        if (containsAny(hay, "marketing", "digital", "brand", "thương hiệu", "truyền thông", "media", "seo")) return "marketing";
        if (containsAny(hay, "security", "an toàn thông tin", "cyber", "owasp", "pentest", "hacker")) return "security";
        if (containsAny(hay, "soft skill", "kỹ năng mềm", "career", "phỏng vấn", "cv", "khởi nghiệp", "startup", "lãnh đạo", "leadership", "tâm lý")) return "softskill";
        if (containsAny(hay, "blockchain", "web3", "crypto", "nft", "smart contract")) return "blockchain";
        if (containsAny(hay, "game", "unity", "unreal")) return "game";
        if (containsAny(hay, "thiết kế", "design", "đồ hoạ", "do hoa", "figma", "photoshop")) return "design";
        return "default";
    }

    private boolean containsAny(String hay, String... needles) {
        for (String n : needles) {
            if (hay.contains(n)) return true;
        }
        return false;
    }

    // ============== Question pool ==============

    private static final class QuestionSpec {
        final String text;
        final String[] options;
        final String correct;
        final int points;
        QuestionSpec(String text, String a, String b, String c, String d, String correct) {
            this(text, a, b, c, d, correct, 1);
        }
        QuestionSpec(String text, String a, String b, String c, String d, String correct, int points) {
            this.text = text;
            this.options = new String[]{a, b, c, d};
            this.correct = correct;
            this.points = points;
        }
    }

    private static final Map<String, List<QuestionSpec>> QUESTION_POOLS = new LinkedHashMap<>();
    static {
        // ===== FPT general — dùng làm câu trộn cho mọi event =====
        QUESTION_POOLS.put("fpt-general", List.of(
            new QuestionSpec("Đại học FPT chính thức thành lập năm nào?",
                "1999", "2006", "2010", "2014", "B"),
            new QuestionSpec("Khẩu hiệu chính thức của FPT University là?",
                "Better Together", "Just Do It", "Bring Your Own Future", "Connecting Future", "C"),
            new QuestionSpec("FPT University hiện có bao nhiêu campus chính trên toàn quốc?",
                "3", "5", "7", "9", "B"),
            new QuestionSpec("Bộ Giáo dục yêu cầu sinh viên đại học tích lũy tối thiểu bao nhiêu tín chỉ để tốt nghiệp (FPT)?",
                "100", "120", "140", "160", "C"),
            new QuestionSpec("Hoạt động ngoại khóa OJT tại FPT viết tắt của?",
                "On Job Training", "Online Java Test", "Open Job Talk", "Office Job Tour", "A"),
            new QuestionSpec("Học kỳ tiếng Nhật tại FPT có tên là?",
                "FOJ", "JFL", "JSP", "NJK", "A"),
            new QuestionSpec("Sự kiện hôm nay được tổ chức bởi đơn vị nào của FPT?",
                "Phòng Công tác sinh viên", "Khoa/Bộ môn chuyên ngành", "Đoàn Thanh niên", "Phòng Đào tạo", "B"),
            new QuestionSpec("Khi tham dự sự kiện, bạn nên đến trước giờ bắt đầu khoảng?",
                "5-10 phút", "30 phút", "1 giờ", "Không cần đến sớm", "A")
        ));

        // ===== AI / ML =====
        QUESTION_POOLS.put("ai", List.of(
            new QuestionSpec("ChatGPT (GPT series) được phát triển bởi công ty nào?",
                "Google", "OpenAI", "Meta", "Microsoft", "B"),
            new QuestionSpec("Mạng nào thường được dùng cho bài toán phân loại ảnh?",
                "Linear Regression", "K-Means", "CNN (Convolutional Neural Network)", "Decision Tree", "C"),
            new QuestionSpec("Library Python nào được dùng phổ biến nhất cho Deep Learning hiện nay?",
                "NumPy", "scikit-learn", "PyTorch", "Pandas", "C"),
            new QuestionSpec("Trong LLM, 'token' nghĩa là gì?",
                "Mã bảo mật", "Đơn vị từ/ký tự được model xử lý", "Tài khoản người dùng", "Giải thưởng", "B"),
            new QuestionSpec("Transformer architecture được giới thiệu trong paper nào?",
                "Deep Learning is Easy", "Attention Is All You Need", "End-to-End Learning", "Backprop Revisited", "B"),
            new QuestionSpec("Quá trình huấn luyện model dùng dữ liệu chưa gán nhãn gọi là?",
                "Supervised Learning", "Unsupervised Learning", "Reinforcement Learning", "Active Learning", "B"),
            new QuestionSpec("Hyperparameter nào điều chỉnh tốc độ học của model?",
                "Batch size", "Learning rate", "Epoch", "Dropout", "B"),
            new QuestionSpec("Hugging Face nổi tiếng với kho mô hình nào?",
                "Computer Vision only", "Pre-trained Transformers", "Reinforcement Learning Agents", "Time-series only", "B")
        ));

        // ===== Cloud =====
        QUESTION_POOLS.put("cloud", List.of(
            new QuestionSpec("Dịch vụ EC2 trong AWS thuộc nhóm dịch vụ gì?",
                "Database", "Compute (máy ảo)", "Storage", "Networking", "B"),
            new QuestionSpec("S3 trong AWS dùng để lưu trữ kiểu dữ liệu nào?",
                "Bảng SQL", "File hệ thống POSIX", "Object storage (file/blob)", "Block storage", "C"),
            new QuestionSpec("Region và Availability Zone (AZ) trong AWS quan hệ thế nào?",
                "Cùng nghĩa", "Mỗi Region chứa nhiều AZ", "Mỗi AZ chứa nhiều Region", "Không liên quan", "B"),
            new QuestionSpec("AWS Lambda là dịch vụ kiểu gì?",
                "Serverless compute", "Block storage", "Relational DB", "DNS", "A"),
            new QuestionSpec("Docker image khác Docker container ở điểm nào?",
                "Image là instance, container là blueprint", "Image là blueprint, container là instance đang chạy", "Hai khái niệm giống hệt nhau", "Container không thể từ image", "B"),
            new QuestionSpec("Kubernetes quản lý đơn vị nhỏ nhất gọi là?",
                "Container", "Pod", "Node", "Service", "B"),
            new QuestionSpec("DevOps nhấn mạnh sự hợp tác giữa hai team nào?",
                "Sales và Marketing", "Development và Operations", "Design và QA", "PM và HR", "B"),
            new QuestionSpec("CI/CD viết tắt của?",
                "Code Integration / Code Deploy", "Continuous Integration / Continuous Delivery", "Cluster Init / Cluster Deploy", "Cloud Infra / Cloud Distribute", "B")
        ));

        // ===== Mobile =====
        QUESTION_POOLS.put("mobile", List.of(
            new QuestionSpec("Ngôn ngữ chính thức cho native iOS development hiện nay là?",
                "Objective-C", "Swift", "Java", "Kotlin", "B"),
            new QuestionSpec("Ngôn ngữ được Google khuyên dùng cho Android hiện đại là?",
                "Java", "Kotlin", "Dart", "C++", "B"),
            new QuestionSpec("Flutter framework do công ty nào phát triển?",
                "Microsoft", "Apple", "Google", "Meta", "C"),
            new QuestionSpec("App Store là kho ứng dụng của hệ điều hành nào?",
                "Android", "iOS", "Windows Phone", "Tizen", "B"),
            new QuestionSpec("React Native dùng ngôn ngữ chính là?",
                "Dart", "JavaScript / TypeScript", "Swift", "Kotlin", "B"),
            new QuestionSpec("Để build cho cả iOS và Android từ một codebase, framework nào KHÔNG phù hợp?",
                "React Native", "Flutter", "ASP.NET MVC", "Ionic", "C"),
            new QuestionSpec("Trên Android, file mô tả permission là?",
                "build.gradle", "AndroidManifest.xml", "package.json", "Info.plist", "B"),
            new QuestionSpec("Trên iOS, file mô tả permission là?",
                "Manifest.xml", "Info.plist", "config.json", "package.swift", "B")
        ));

        // ===== Web / Frontend =====
        QUESTION_POOLS.put("web", List.of(
            new QuestionSpec("React được phát triển và mở mã nguồn bởi?",
                "Google", "Meta (Facebook)", "Microsoft", "Twitter", "B"),
            new QuestionSpec("CSS framework utility-first đang được dùng phổ biến là?",
                "Bootstrap", "Tailwind CSS", "Foundation", "Bulma", "B"),
            new QuestionSpec("Build tool hiện đại thay thế Webpack trong nhiều dự án mới là?",
                "Vite", "Grunt", "Gulp", "Parcel", "A"),
            new QuestionSpec("HTML5 ra mắt chính thức năm nào?",
                "2008", "2012", "2014", "2016", "C"),
            new QuestionSpec("Trong Vue 3, API được khuyến nghị là?",
                "Options API", "Composition API", "Class API", "Decorator API", "B"),
            new QuestionSpec("HTTP method nào idempotent (gọi nhiều lần kết quả không đổi)?",
                "POST", "GET", "PATCH", "CONNECT", "B"),
            new QuestionSpec("CORS viết tắt của?",
                "Cross-Origin Resource Sharing", "Common Object Routing Service", "Client Origin Restriction System", "Cross-Object Render Sequence", "A"),
            new QuestionSpec("JSON Web Token (JWT) gồm mấy phần ngăn cách dấu chấm?",
                "2", "3", "4", "5", "B")
        ));

        // ===== Backend =====
        QUESTION_POOLS.put("backend", List.of(
            new QuestionSpec("Annotation @RestController trong Spring Boot nằm ở package?",
                "org.springframework.web.bind.annotation", "org.springframework.beans", "org.springframework.core", "org.springframework.context", "A"),
            new QuestionSpec("JPA viết tắt của?",
                "Java Persist Adapter", "Java Persistence API", "Just POJO Annotation", "Java Plain Access", "B"),
            new QuestionSpec("Spring Boot starter cho web là dependency nào?",
                "spring-boot-starter-web", "spring-web-starter", "spring-boot-web", "web-starter", "A"),
            new QuestionSpec("SQL Server thuộc loại database nào?",
                "NoSQL", "Relational (RDBMS)", "Graph", "Time-series", "B"),
            new QuestionSpec("REST API thường trả status code 201 nghĩa là?",
                "OK", "Created", "Accepted", "No Content", "B"),
            new QuestionSpec("ORM viết tắt của?",
                "Object Relational Mapping", "Online Record Manager", "Open Routing Module", "Output Render Method", "A"),
            new QuestionSpec("Trong Spring, scope mặc định của bean là?",
                "prototype", "singleton", "request", "session", "B"),
            new QuestionSpec("Index trong database giúp?",
                "Tăng tốc INSERT", "Tăng tốc query SELECT", "Giảm dung lượng DB", "Giảm số bảng", "B")
        ));

        // ===== Marketing =====
        QUESTION_POOLS.put("marketing", List.of(
            new QuestionSpec("CTR trong digital marketing viết tắt của?",
                "Click Through Rate", "Cost To Run", "Content Targeting Ratio", "Click Total Revenue", "A"),
            new QuestionSpec("KOL viết tắt của?",
                "Key Online Leader", "Key Opinion Leader", "Key Operating Logic", "Knowledge Output Layer", "B"),
            new QuestionSpec("Marketing funnel cổ điển có mấy giai đoạn chính?",
                "2", "3", "4 (AIDA: Awareness-Interest-Desire-Action)", "5", "C"),
            new QuestionSpec("ROI viết tắt của?",
                "Return On Investment", "Rate Of Income", "Real Online Index", "Run Of Industry", "A"),
            new QuestionSpec("Persona trong marketing nghĩa là?",
                "Một sản phẩm cụ thể", "Chân dung khách hàng mục tiêu mô phỏng", "Một chiến dịch quảng cáo", "Một kênh truyền thông", "B"),
            new QuestionSpec("SEO là viết tắt của?",
                "Search Engine Optimization", "Social Engagement Online", "Service Experience Operation", "Sponsored Engagement Output", "A"),
            new QuestionSpec("CTA trong marketing là?",
                "Call To Action", "Click Through Average", "Customer Total Acquisition", "Convert Tag Adapter", "A"),
            new QuestionSpec("Facebook Ads Manager thường tính phí dựa trên mô hình nào?",
                "Mua trọn gói tháng", "CPC / CPM / CPA", "Trả 1 lần cho cả năm", "Miễn phí hoàn toàn", "B")
        ));

        // ===== Soft Skill / Career =====
        QUESTION_POOLS.put("softskill", List.of(
            new QuestionSpec("Phương pháp STAR thường dùng để?",
                "Học từ vựng", "Trả lời câu hỏi phỏng vấn hành vi", "Lập trình", "Thiết kế UI", "B"),
            new QuestionSpec("Pomodoro Technique là phương pháp về?",
                "Nấu ăn", "Quản lý thời gian học/làm việc theo phiên 25 phút", "Học ngoại ngữ", "Tài chính cá nhân", "B"),
            new QuestionSpec("Mục tiêu SMART, chữ S đầu nghĩa là?",
                "Strong", "Specific (cụ thể)", "Simple", "Solid", "B"),
            new QuestionSpec("CV viết tắt của?",
                "Common Version", "Curriculum Vitae", "Career Value", "Concept Volume", "B"),
            new QuestionSpec("Khi viết email gửi nhà tuyển dụng, KHÔNG nên?",
                "Mở đầu lịch sự", "Tóm tắt rõ mục đích", "Dùng tiếng lóng/biểu tượng cảm xúc", "Kèm CV file PDF", "C"),
            new QuestionSpec("Ma trận Eisenhower phân loại công việc theo 2 trục?",
                "Vui-Buồn", "Cấp bách-Quan trọng", "Khó-Dễ", "Nhanh-Chậm", "B"),
            new QuestionSpec("OKR là phương pháp đặt mục tiêu nào?",
                "Objectives and Key Results", "Online Knowledge Routine", "Optimal Key Reach", "Output Keep Repeating", "A"),
            new QuestionSpec("Trong giao tiếp, lắng nghe chủ động (Active Listening) đòi hỏi?",
                "Im lặng cho qua chuyện", "Tập trung, phản hồi, đặt câu hỏi làm rõ", "Vừa nghe vừa lướt điện thoại", "Cắt lời để rút ngắn", "B")
        ));

        // ===== Cybersecurity =====
        QUESTION_POOLS.put("security", List.of(
            new QuestionSpec("OWASP nổi tiếng với danh sách Top 10 loại lỗi nào?",
                "Lỗi UI", "Lỗ hổng bảo mật ứng dụng web", "Bug database", "Sai sót thiết kế", "B"),
            new QuestionSpec("SQL Injection là dạng tấn công?",
                "Server crash", "Tiêm SQL độc hại qua input người dùng", "Brute-force mật khẩu", "Man-in-the-middle", "B"),
            new QuestionSpec("HTTPS dùng giao thức mã hoá nào?",
                "SSL/TLS", "SSH", "PGP", "MD5", "A"),
            new QuestionSpec("2FA giúp tăng cường yếu tố gì?",
                "Tốc độ", "Bảo mật đăng nhập", "Hiệu suất server", "Trải nghiệm UX", "B"),
            new QuestionSpec("Cấp độ phân quyền tối thiểu cần thiết là nguyên tắc?",
                "Least Privilege", "Maximum Power", "All Or Nothing", "Open Default", "A"),
            new QuestionSpec("Phishing là kiểu tấn công?",
                "Đoán mật khẩu", "Lừa người dùng tiết lộ thông tin qua email/web giả", "Tấn công DDoS", "Khai thác buffer overflow", "B"),
            new QuestionSpec("Mã hoá đối xứng nghĩa là?",
                "Encrypt và decrypt dùng cùng 1 key", "Mỗi bên 1 cặp key public/private", "Không cần key", "Chỉ encrypt được, không decrypt", "A"),
            new QuestionSpec("MFA viết tắt của?",
                "Multi-Factor Authentication", "Master File Access", "Manual File Approval", "Modern Firewall Architecture", "A")
        ));

        // ===== Blockchain =====
        QUESTION_POOLS.put("blockchain", List.of(
            new QuestionSpec("Bitcoin được giới thiệu năm nào qua whitepaper?",
                "2005", "2008", "2010", "2013", "B"),
            new QuestionSpec("Smart Contract phổ biến nhất chạy trên blockchain nào?",
                "Bitcoin", "Ethereum", "Tron", "Cardano", "B"),
            new QuestionSpec("NFT viết tắt của?",
                "Non-Fungible Token", "New Finance Tech", "Network File Transfer", "Native Future Token", "A"),
            new QuestionSpec("Gas fee trên blockchain là?",
                "Phí trả cho thợ đào để xử lý giao dịch", "Phí ngân hàng", "Tiền lương developer", "Thuế nhà nước", "A")
        ));

        // ===== Game =====
        QUESTION_POOLS.put("game", List.of(
            new QuestionSpec("Unity engine dùng ngôn ngữ chính nào để viết script?",
                "C++", "C#", "Lua", "Python", "B"),
            new QuestionSpec("Unreal Engine dùng ngôn ngữ chính nào?",
                "Java", "C++", "Python", "JavaScript", "B"),
            new QuestionSpec("FPS trong game là viết tắt của?",
                "First Person Shooter / Frames Per Second", "Free Player Service", "Fast Pack System", "Full Path Server", "A"),
            new QuestionSpec("Game engine nào miễn phí cho indie với royalty thấp?",
                "Unity Personal", "Frostbite", "RAGE", "REDengine", "A")
        ));

        // ===== Design =====
        QUESTION_POOLS.put("design", List.of(
            new QuestionSpec("Figma chủ yếu được dùng để?",
                "Lập trình mobile", "Thiết kế UI/UX cộng tác online", "Render 3D", "Edit video", "B"),
            new QuestionSpec("Hệ màu RGB dùng trong?",
                "In ấn", "Màn hình điện tử", "Vẽ tay", "Sơn dầu", "B"),
            new QuestionSpec("Typography là nghệ thuật về?",
                "Bố cục ảnh", "Font chữ và cách trình bày chữ", "Phối màu", "Animation", "B"),
            new QuestionSpec("Design Thinking gồm bao nhiêu giai đoạn chính?",
                "3", "5 (Empathize-Define-Ideate-Prototype-Test)", "7", "9", "B")
        ));

        // ===== Default — pha thêm câu chung kỹ thuật =====
        QUESTION_POOLS.put("default", List.of(
            new QuestionSpec("Git là công cụ?",
                "Quản lý phiên bản mã nguồn (VCS)", "Editor code", "Web browser", "Database", "A"),
            new QuestionSpec("Pull Request trên GitHub dùng để?",
                "Tải code về máy", "Đề xuất merge code vào branch khác", "Xoá repository", "Tạo issue mới", "B"),
            new QuestionSpec("Markdown được dùng phổ biến để?",
                "Viết tài liệu định dạng nhẹ", "Lập trình OOP", "Quản lý DB", "Render 3D", "A"),
            new QuestionSpec("Stack Overflow là?",
                "Một game", "Cộng đồng hỏi đáp lập trình", "Một IDE", "Cloud provider", "B"),
            new QuestionSpec("Open source nghĩa là?",
                "Có phí ẩn", "Mã nguồn mở, ai cũng có thể xem/sử dụng theo giấy phép", "Chỉ chạy được trên Linux", "Không có hỗ trợ", "B"),
            new QuestionSpec("Docker giúp giải quyết vấn đề nào?",
                "Đồng bộ môi trường chạy ứng dụng", "Thiết kế UI", "Vẽ flowchart", "Soạn văn bản", "A"),
            new QuestionSpec("API thường trao đổi dữ liệu phổ biến theo format?",
                "PDF", "JSON / XML", "DOCX", "MP3", "B"),
            new QuestionSpec("OOP gồm 4 trụ cột, trong đó KHÔNG có?",
                "Encapsulation", "Inheritance", "Polymorphism", "Recursion", "D")
        ));
    }
}
