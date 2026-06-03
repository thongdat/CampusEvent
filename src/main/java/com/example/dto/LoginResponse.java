package com.example.dto;

public class LoginResponse {

    private boolean success;
    private String message;
    private UserInfo user;

    public LoginResponse() {}

    private String errorType;

    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.user = null;
        this.errorType = null;
    }

    public LoginResponse(boolean success, String message, UserInfo user) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.errorType = null;
    }

    public LoginResponse(boolean success, String message, UserInfo user, String errorType) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.errorType = errorType;
    }

    // --- Static factory methods ---

    public static LoginResponse success(String message, UserInfo user) {
        return new LoginResponse(true, message, user);
    }

    public static LoginResponse error(String message, String errorType) {
        return new LoginResponse(false, message, null, errorType);
    }

    public static LoginResponse error(String message) {
        return error(message, null);
    }

    // --- Getters & Setters ---

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    // --- Inner class: thông tin user trả về cho frontend ---
    public static class UserInfo {
        private Long id;
        private String fullName;
        private String email;
        private String role;
        private String major;
        private String faculty;
        private String departmentPosition;

        public UserInfo() {}

        public UserInfo(Long id, String fullName, String email, String role) {
            this(id, fullName, email, role, null, null);
        }

        public UserInfo(Long id, String fullName, String email, String role, String major, String faculty) {
            this(id, fullName, email, role, major, faculty, null);
        }

        public UserInfo(Long id, String fullName, String email, String role, String major, String faculty, String departmentPosition) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
            this.major = major;
            this.faculty = faculty;
            this.departmentPosition = departmentPosition;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getMajor() { return major; }
        public void setMajor(String major) { this.major = major; }

        public String getFaculty() { return faculty; }
        public void setFaculty(String faculty) { this.faculty = faculty; }

        public String getDepartmentPosition() { return departmentPosition; }
        public void setDepartmentPosition(String departmentPosition) { this.departmentPosition = departmentPosition; }
    }
}
