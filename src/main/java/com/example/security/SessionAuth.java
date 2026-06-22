package com.example.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Lưu/đọc danh tính người dùng trong HTTP session phía server.
 * Đây là nguồn danh tính TIN CẬY cho phân quyền — client không thể giả mạo
 * như header tự khai, vì session gắn với cookie do server cấp khi đăng nhập.
 */
public final class SessionAuth {

    public static final String USER_ID = "AUTH_USER_ID";
    public static final String EMAIL = "AUTH_EMAIL";
    public static final String ROLE = "AUTH_ROLE";

    private SessionAuth() {
    }

    /** Gọi sau khi xác thực thành công (đăng nhập mật khẩu hoặc Google). */
    public static void set(HttpServletRequest request, Long userId, String email, String role) {
        HttpSession session = request.getSession(true);
        session.setAttribute(USER_ID, userId);
        session.setAttribute(EMAIL, email);
        session.setAttribute(ROLE, role == null ? null : role.trim().toUpperCase());
    }

    public static String role(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(ROLE);
    }

    public static String email(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(EMAIL);
    }

    public static void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
