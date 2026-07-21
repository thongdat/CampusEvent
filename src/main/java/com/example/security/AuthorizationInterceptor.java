package com.example.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Phân quyền theo vai trò ở tầng server (RBAC).
 *
 * Chỉ chạy cho các nhóm API nội bộ (đăng ký pattern trong WebSecurityMvcConfig):
 *   /admin/**, /committee/**, /department/**, /student/**
 *
 * Quy tắc:
 *   - Chưa đăng nhập (không có phiên)         -> 401
 *   - Đăng nhập nhưng sai vai trò             -> 403
 *
 * Lưu ý về namespace /admin (bị dùng chung):
 *   - Quản trị nhạy cảm (users/roles/email-logs/activity-logs/registrations/feedback,
 *     overview, reports, và GHI departments) -> chỉ ADMIN.
 *   - Phần còn lại (events/proposals/dashboard + ĐỌC departments) -> ADMIN/MANAGER/DEPARTMENT,
 *     vì Department Console & AEMS Toolkit gọi các endpoint /admin/events, /admin/proposals,
 *     /admin/dashboard, /admin/departments(GET).
 */
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        String path = stripContextPath(request);
        String method = request.getMethod();
        String role = SessionAuth.role(request);

        if (role == null) {
            return deny(request, response, 401, "Bạn cần đăng nhập để tiếp tục.");
        }
        role = role.toUpperCase();

        if (path.startsWith("/student")) {
            if (!allow(request, response, role, "STUDENT", "ADMIN")) {
                return false;
            }
            return assertOwnIdentity(request, response, role);
        }
        if (path.startsWith("/committee")) {
            if (!allow(request, response, role, "COMMITTEE", "ADMIN")) {
                return false;
            }
            return assertOwnIdentity(request, response, role);
        }
        if (path.startsWith("/department")) {
            return allow(request, response, role, "DEPARTMENT", "MANAGER", "ADMIN");
        }
        if (path.startsWith("/admin")) {
            if (isAdminOnly(path, method)) {
                return allow(request, response, role, "ADMIN");
            }
            // Hội đồng duyệt cần ĐỌC danh sách phòng để tạo event công khai khi phê duyệt.
            if ("GET".equalsIgnoreCase(method) && path.startsWith("/admin/rooms/options")) {
                return allow(request, response, role, "ADMIN", "MANAGER", "DEPARTMENT", "COMMITTEE");
            }
            return allow(request, response, role, "ADMIN", "MANAGER", "DEPARTMENT");
        }
        return true;
    }

    /**
     * Chống mạo danh cùng vai (horizontal): nếu client gửi X-User-Email khác với email
     * trong phiên thì chặn — trừ ADMIN (được phép thao tác thay người dùng khác).
     * Không gửi header thì bỏ qua (không phá các luồng cũ).
     */
    private boolean assertOwnIdentity(HttpServletRequest request, HttpServletResponse response, String role)
            throws IOException {
        if ("ADMIN".equals(role)) {
            return true;
        }
        String headerEmail = request.getHeader("X-User-Email");
        String sessionEmail = SessionAuth.email(request);
        if (headerEmail != null && !headerEmail.isBlank()
                && sessionEmail != null
                && !headerEmail.trim().equalsIgnoreCase(sessionEmail.trim())) {
            return deny(request, response, 403, "Bạn không thể truy cập dữ liệu của người dùng khác.");
        }
        return true;
    }

    /** Các endpoint quản trị nhạy cảm chỉ dành cho ADMIN. */
    private boolean isAdminOnly(String path, String method) {
        if (path.startsWith("/admin/users")
                || path.startsWith("/admin/roles")
                || path.startsWith("/admin/email-logs")
                || path.startsWith("/admin/activity-logs")
                || path.startsWith("/admin/registrations")
                || path.startsWith("/admin/feedback")
                || path.equals("/admin/overview")
                || path.equals("/admin/reports")) {
            return true;
        }
        // GET /admin/departments dùng để đổ dropdown cho Department Console -> cho phép;
        // còn POST/PUT/DELETE thì chỉ ADMIN.
        if (path.startsWith("/admin/departments")) {
            return !"GET".equalsIgnoreCase(method);
        }
        return false;
    }

    private boolean allow(HttpServletRequest request, HttpServletResponse response, String role,
                          String... allowedRoles) throws IOException {
        for (String allowed : allowedRoles) {
            if (allowed.equals(role)) {
                return true;
            }
        }
        return deny(request, response, 403, "Bạn không có quyền truy cập chức năng này.");
    }

    private boolean deny(HttpServletRequest request, HttpServletResponse response, int status, String message)
            throws IOException {
        // Trình duyệt mở trực tiếp trang HTML server-render mà chưa đăng nhập (401)
        // -> đưa về trang đăng nhập cho thân thiện, thay vì trả JSON thô.
        if (status == 401 && isHtmlNavigation(request)) {
            response.sendRedirect("/api/login.html");
            return false;
        }
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"error\":\"" + message + "\",\"message\":\"" + message + "\"}");
        return false;
    }

    private boolean isHtmlNavigation(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }

    private String stripContextPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        return uri;
    }
}
