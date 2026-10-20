package com.networkfinder.model;

import java.time.LocalDateTime;

public class User {
    private String studentId; // Used as username for students, or "admin" for administrators
    private String fullName;
    private String passwordHash;
    private String idCardImagePath; // Relative or absolute path to uploaded ID card photo
    private Role role;
    private VerificationStatus verificationStatus;
    private boolean profileCompleted;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(String studentId, String fullName, String passwordHash, String idCardImagePath, 
                Role role, VerificationStatus verificationStatus, boolean profileCompleted, LocalDateTime createdAt) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.idCardImagePath = idCardImagePath;
        this.role = role;
        this.verificationStatus = verificationStatus;
        this.profileCompleted = profileCompleted;
        this.createdAt = createdAt;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getIdCardImagePath() {
        return idCardImagePath;
    }

    public void setIdCardImagePath(String idCardImagePath) {
        this.idCardImagePath = idCardImagePath;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return fullName + " (" + studentId + ")";
    }
}
