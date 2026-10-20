package com.networkfinder.model;

import java.time.LocalDateTime;

public class User {
    private String studentId; // Used as username for students, or "admin" for administrators
    private String firstName;
    private String middleInitial;
    private String surname;
    private String suffix;
    private String fullName; // Composed display name derived from the name parts
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

    /**
     * Stores the individual name parts and keeps the legacy display-name
     * column in sync. Empty parts (middle initial, suffix) are simply skipped
     * during composition: "J. A. Reyes Jr.".
     */
    public void setNames(String firstName, String middleInitial, String surname, String suffix) {
        this.firstName = firstName;
        this.middleInitial = middleInitial;
        this.surname = surname;
        this.suffix = suffix;
        this.fullName = composeFullName(firstName, middleInitial, surname, suffix);
    }

    public static String composeFullName(String firstName, String middleInitial, String surname, String suffix) {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            sb.append(firstName.trim());
        }
        if (middleInitial != null && !middleInitial.isBlank()) {
            appendSpaceIfNeeded(sb).append(middleInitial.trim());
        }
        if (surname != null && !surname.isBlank()) {
            appendSpaceIfNeeded(sb).append(surname.trim());
        }
        if (suffix != null && !suffix.isBlank()) {
            appendSpaceIfNeeded(sb).append(suffix.trim());
        }
        return sb.toString();
    }

    private static StringBuilder appendSpaceIfNeeded(StringBuilder sb) {
        if (sb.length() > 0) {
            sb.append(' ');
        }
        return sb;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleInitial() {
        return middleInitial;
    }

    public void setMiddleInitial(String middleInitial) {
        this.middleInitial = middleInitial;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
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
