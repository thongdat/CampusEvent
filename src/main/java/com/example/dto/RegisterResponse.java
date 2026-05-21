package com.example.dto;

public class RegisterResponse {

    private boolean success;
    private String message;
    private UserInfo user;

    public RegisterResponse() {}

    public RegisterResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.user = null;
    }

    public RegisterResponse(boolean success, String message, UserInfo user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    // Static factory methods
    public static RegisterResponse success(String message, UserInfo user) {
        return new RegisterResponse(true, message, user);
    }

    public static RegisterResponse error(String message) {
        return new RegisterResponse(false, message);
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }

    // Inner class for user info
    public static class UserInfo {
        private Long id;
        private String fullName;
        private String email;
        private String role;
        private String major;
        private String faculty;

        public UserInfo() {}

        public UserInfo(Long id, String fullName, String email, String role) {
            this(id, fullName, email, role, null, null);
        }

        public UserInfo(Long id, String fullName, String email, String role, String major, String faculty) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
            this.major = major;
            this.faculty = faculty;
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
    }
}
