package com.example.service;

import com.example.config.AcademicStructure;
import com.example.dto.LoginRequest;
import com.example.dto.LoginResponse;
import com.example.dto.RegisterRequest;
import com.example.dto.RegisterResponse;
import com.example.model.ActivityLog;
import com.example.model.Role;
import com.example.model.Student;
import com.example.model.User;
import com.example.repository.ActivityLogRepository;
import com.example.repository.RoleRepository;
import com.example.repository.StudentRepository;
import com.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final int EMAIL_LOGIN_POINTS = 2;
    private static final int REGISTER_POINTS = 10;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ConcurrentHashMap<String, Map<String, Object>> pendingOtps = new ConcurrentHashMap<>();

    public LoginResponse login(LoginRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());

        if (optionalUser.isEmpty()) {
            return LoginResponse.error("Email hoặc mật khẩu không chính xác.", "INVALID_CREDENTIALS");
        }

        User user = optionalUser.get();
        String stored = user.getPassword() == null ? "" : user.getPassword();
        String input = request.getPassword() == null ? "" : request.getPassword();

        boolean matched;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            matched = passwordEncoder.matches(input, stored);
        } else if (stored.startsWith("plain:")) {
            // Seed password chưa được hash. Nếu khớp thì tự BCrypt rồi save lại,
            // tránh việc user phải gọi /api/auth/init-passwords thủ công.
            String literal = stored.substring("plain:".length());
            matched = literal.equals(input);
            if (matched) {
                user.setPassword(passwordEncoder.encode(literal));
                userRepository.save(user);
            }
        } else {
            // Legacy "hashed_password_xxx" — không có cách verify, từ chối an toàn.
            matched = false;
        }

        if (!matched) {
            return LoginResponse.error("Email hoặc mật khẩu không chính xác.", "INVALID_CREDENTIALS");
        }

        if (!Boolean.TRUE.equals(user.getStatus())) {
            return LoginResponse.error("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.", "ACCOUNT_LOCKED");
        }

        ActivityLog log = new ActivityLog(user, "EMAIL_LOGIN", "Đăng nhập bằng email", EMAIL_LOGIN_POINTS);
        activityLogRepository.save(log);
        user.setTotalPoints((user.getTotalPoints() != null ? user.getTotalPoints() : 0) + EMAIL_LOGIN_POINTS);
        userRepository.save(user);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getName(),
                user.getMajor(),
                AcademicStructure.facultyOf(user.getMajor())
        );

        return LoginResponse.success("Đăng nhập thành công!", userInfo);
    }

    private String generateOtp() {
        int otp = 100000 + new java.util.Random().nextInt(900000);
        return String.valueOf(otp);
    }

    public Map<String, Object> sendRegistrationOtp(String email) {
        Map<String, Object> result = new HashMap<>();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();

        if (normalizedEmail.isEmpty()) {
            result.put("success", false);
            result.put("message", "Vui lòng nhập email.");
            return result;
        }

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            result.put("success", false);
            result.put("message", "Email đã được sử dụng. Vui lòng chọn email khác hoặc đăng nhập.");
            return result;
        }

        String otpCode = generateOtp();
        Map<String, Object> otpData = new HashMap<>();
        otpData.put("otp", otpCode);
        otpData.put("expiry", LocalDateTime.now().plusMinutes(5));
        pendingOtps.put(normalizedEmail, otpData);

        emailService.sendRegistrationOtpEmail(normalizedEmail, otpCode);

        result.put("success", true);
        result.put("message", "Mã OTP đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư.");
        return result;
    }

    public RegisterResponse register(RegisterRequest request) {
        String password = request.getPassword() == null ? "" : request.getPassword();
        String confirmPassword = request.getConfirmPassword() == null ? "" : request.getConfirmPassword();
        if (!password.equals(confirmPassword)) {
            return RegisterResponse.error("Mật khẩu xác nhận không khớp.");
        }

        String roleName = request.getRole() == null ? "" : request.getRole().trim().toUpperCase();
        if (!"STUDENT".equals(roleName) && !"DEPARTMENT".equals(roleName)) {
            return RegisterResponse.error("Vai trò không hợp lệ. Chỉ được phép đăng ký với vai trò Sinh viên hoặc Khoa/Bộ môn.");
        }

        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            return RegisterResponse.error("Email đã được sử dụng. Vui lòng chọn email khác.");
        }

        String phone = request.getPhone() == null ? "" : request.getPhone().trim();
        if (phone.isEmpty()) {
            return RegisterResponse.error("Vui lòng nhập số điện thoại.");
        }
        if (!phone.matches("^\\d{10}$")) {
            return RegisterResponse.error("Số điện thoại phải gồm đúng 10 chữ số.");
        }
        if (userRepository.findByPhone(phone).isPresent()) {
            return RegisterResponse.error("Số điện thoại đã được sử dụng. Mỗi người chỉ được dùng một số điện thoại.");
        }

        String otpCode = request.getOtpCode();
        if (otpCode == null || otpCode.trim().isEmpty()) {
            return RegisterResponse.error("Vui lòng nhập mã OTP xác minh email.");
        }

        Map<String, Object> otpData = pendingOtps.get(email);
        if (otpData == null) {
            return RegisterResponse.error("Chưa có mã OTP nào được gửi. Vui lòng yêu cầu gửi lại.");
        }

        LocalDateTime expiry = (LocalDateTime) otpData.get("expiry");
        String storedOtp = (String) otpData.get("otp");

        if (LocalDateTime.now().isAfter(expiry)) {
            pendingOtps.remove(email);
            return RegisterResponse.error("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }

        if (!otpCode.trim().equals(storedOtp)) {
            return RegisterResponse.error("Mã OTP không chính xác.");
        }

        String selectedDepartment = "";
        String selectedFaculty = "";
        if ("STUDENT".equals(roleName) || "DEPARTMENT".equals(roleName)) {
            selectedDepartment = request.getMajor() == null ? "" : request.getMajor().trim();
            selectedFaculty = request.getFaculty() == null ? "" : request.getFaculty().trim();
            if (selectedDepartment.isEmpty()) {
                return RegisterResponse.error("Vui lòng chọn khoa/bộ môn.");
            }
            if (!AcademicStructure.isKnownDepartment(selectedDepartment)) {
                return RegisterResponse.error("Khoa/bộ môn không hợp lệ.");
            }
            selectedDepartment = AcademicStructure.canonicalDepartment(selectedDepartment);
            if (!selectedFaculty.isEmpty() && !AcademicStructure.belongsToFaculty(selectedFaculty, selectedDepartment)) {
                return RegisterResponse.error("Bộ môn không thuộc khoa đã chọn.");
            }
            selectedFaculty = AcademicStructure.facultyOf(selectedDepartment);
        }

        String studentCode = "";
        if ("STUDENT".equals(roleName)) {
            studentCode = request.getStudentCode() == null ? "" : request.getStudentCode().trim().toUpperCase();
            if (studentCode.isEmpty()) {
                return RegisterResponse.error("Vui lòng nhập mã sinh viên.");
            }
            if (studentCode.matches("^SV\\d+$")) {
                return RegisterResponse.error("Không sử dụng mã tự sinh kiểu SV001. Vui lòng nhập mã sinh viên thật.");
            }
            if (!studentCode.matches("^[A-Z0-9]{4,20}$")) {
                return RegisterResponse.error("Mã sinh viên chỉ gồm chữ và số, dài từ 4 đến 20 ký tự.");
            }
            if (studentRepository.findByStudentCode(studentCode).isPresent()) {
                return RegisterResponse.error("Mã sinh viên đã tồn tại. Mỗi sinh viên chỉ có một mã sinh viên.");
            }
            if (request.getSemester() == null || request.getSemester() < 1 || request.getSemester() > 9) {
                return RegisterResponse.error("Kì học phải từ 1 đến 9.");
            }
        }

        Role selectedRole = roleRepository.findByName(roleName);
        if (selectedRole == null) {
            return RegisterResponse.error("Lỗi hệ thống. Vai trò không tồn tại trong database.");
        }

        pendingOtps.remove(email);

        User newUser = new User();
        newUser.setFullName(request.getFullName().trim());
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setPhone(phone);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setStatus(true);
        newUser.setRole(selectedRole);
        newUser.setTotalPoints(REGISTER_POINTS);

        if ("STUDENT".equals(roleName) || "DEPARTMENT".equals(roleName)) {
            newUser.setMajor(selectedDepartment);
        }
        if ("STUDENT".equals(roleName)) {
            newUser.setSemester(request.getSemester());
        }

        try {
            User savedUser = userRepository.save(newUser);

            if ("STUDENT".equals(roleName)) {
                Student student = new Student(studentCode, selectedDepartment, request.getSemester(), savedUser);
                studentRepository.save(student);
            }

            String activityType = request.isFromGoogle() ? "GOOGLE_REGISTER" : "EMAIL_REGISTER";
            String description = request.isFromGoogle()
                    ? "Đăng ký tài khoản mới qua Google OAuth"
                    : "Đăng ký tài khoản mới qua email";
            activityLogRepository.save(new ActivityLog(savedUser, activityType, description, REGISTER_POINTS));

            RegisterResponse.UserInfo userInfo = new RegisterResponse.UserInfo(
                    savedUser.getId(),
                    savedUser.getFullName(),
                    savedUser.getEmail(),
                    savedUser.getRole().getName(),
                    savedUser.getMajor(),
                    selectedFaculty
            );

            return RegisterResponse.success("Đăng ký thành công! Bạn có thể đăng nhập ngay.", userInfo);
        } catch (Exception e) {
            logger.error("Lỗi khi lưu user đăng ký", e);
            return RegisterResponse.error("Lỗi hệ thống. Vui lòng thử lại sau.");
        }
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public Map<String, Object> forgotPassword(String email) {
        Map<String, Object> result = new HashMap<>();
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            result.put("success", false);
            result.put("message", "Email không tồn tại trong hệ thống.");
            return result;
        }

        User user = optionalUser.get();
        String otpCode = generateOtp();
        user.setOtpCode(otpCode);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        emailService.sendOtpEmail(email, otpCode);

        result.put("success", true);
        result.put("message", "Mã OTP đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư.");
        return result;
    }

    public Map<String, Object> verifyOtp(String email, String otpCode) {
        Map<String, Object> result = new HashMap<>();
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            result.put("success", false);
            result.put("message", "Email không tồn tại.");
            return result;
        }

        User user = optionalUser.get();
        if (user.getOtpCode() == null || user.getOtpExpiry() == null) {
            result.put("success", false);
            result.put("message", "Chưa có mã OTP nào được gửi. Vui lòng yêu cầu lại.");
            return result;
        }

        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            user.setOtpCode(null);
            user.setOtpExpiry(null);
            userRepository.save(user);
            result.put("success", false);
            result.put("message", "Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
            return result;
        }

        if (!otpCode.equals(user.getOtpCode())) {
            result.put("success", false);
            result.put("message", "Mã OTP không chính xác.");
            return result;
        }

        result.put("success", true);
        result.put("message", "Xác minh thành công! Bạn có thể đặt lại mật khẩu.");
        return result;
    }

    public Map<String, Object> resetPassword(String email, String otpCode, String newPassword) {
        Map<String, Object> result = new HashMap<>();
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            result.put("success", false);
            result.put("message", "Email không tồn tại.");
            return result;
        }

        User user = optionalUser.get();
        if (user.getOtpCode() == null || !otpCode.equals(user.getOtpCode())) {
            result.put("success", false);
            result.put("message", "Mã OTP không hợp lệ.");
            return result;
        }

        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            result.put("success", false);
            result.put("message", "Mã OTP đã hết hạn.");
            return result;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        result.put("success", true);
        result.put("message", "Mật khẩu đã được đặt lại thành công! Bạn có thể đăng nhập ngay.");
        return result;
    }

    /**
     * Hash lại các mật khẩu seed trong database.
     * - Nếu password bắt đầu bằng "plain:abc123" → hash "abc123" (giữ mật khẩu riêng).
     *   Cho phép phân biệt admin123 / dept123 / com123 / stu123 trong seed FPT.
     * - Nếu password đã ở dạng BCrypt ($2a$) → bỏ qua.
     * - Trường hợp khác (legacy "hashed_password_xxx") → fallback "12345678".
     */
    public Map<String, Object> initSeedPasswords() {
        Map<String, Object> result = new HashMap<>();
        String fallback = "12345678";
        List<User> allUsers = userRepository.findAll();
        int count = 0;

        for (User user : allUsers) {
            String pwd = user.getPassword();
            if (pwd == null || pwd.startsWith("$2a$")) {
                continue;
            }
            String target;
            if (pwd.startsWith("plain:") && pwd.length() > "plain:".length()) {
                target = pwd.substring("plain:".length());
            } else {
                target = fallback;
            }
            user.setPassword(passwordEncoder.encode(target));
            userRepository.save(user);
            count++;
        }

        result.put("success", true);
        result.put("message", "Đã cập nhật " + count + " tài khoản. Mật khẩu seed prefix 'plain:' được giữ nguyên, các tài khoản cũ dùng fallback '" + fallback + "'.");
        result.put("updatedCount", count);
        return result;
    }
}
