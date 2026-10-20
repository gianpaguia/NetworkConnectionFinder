package com.networkfinder.service;

import com.networkfinder.database.DatabaseManager;
import com.networkfinder.database.UserDao;
import com.networkfinder.model.Role;
import com.networkfinder.model.User;
import com.networkfinder.model.VerificationStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

public class AuthService {
    public static final long MAX_ID_IMAGE_BYTES = 5 * 1024 * 1024; // 5 MB upload cap

    private final UserDao userDao = new UserDao();

    public enum AuthStatus {
        SUCCESS,
        INVALID_CREDENTIALS,
        PENDING_VERIFICATION,
        ACCOUNT_REJECTED,
        USER_NOT_FOUND
    }

    public static class LoginResult {
        private final AuthStatus status;
        private final User user;
        private final String message;

        public LoginResult(AuthStatus status, User user, String message) {
            this.status = status;
            this.user = user;
            this.message = message;
        }

        public AuthStatus getStatus() {
            return status;
        }

        public User getUser() {
            return user;
        }

        public String getMessage() {
            return message;
        }
    }

    public LoginResult authenticate(String studentId, String password) {
        if (studentId == null || studentId.isBlank() || password == null || password.isBlank()) {
            return new LoginResult(AuthStatus.INVALID_CREDENTIALS, null, "Please enter both ID and password.");
        }

        User user = userDao.getUserById(studentId.trim());
        if (user == null) {
            return new LoginResult(AuthStatus.USER_NOT_FOUND, null, "No account found with this Student ID.");
        }

        if (!DatabaseManager.verifyPassword(password, user.getPasswordHash())) {
            return new LoginResult(AuthStatus.INVALID_CREDENTIALS, null, "Incorrect password.");
        }

        // Upgrade legacy unsalted SHA-256 hashes to PBKDF2 on successful login
        if (DatabaseManager.isLegacyHash(user.getPasswordHash())) {
            userDao.updatePasswordHash(user.getStudentId(), DatabaseManager.hashPassword(password));
        }

        // Students must be approved before logging in
        if (user.getRole() == Role.STUDENT) {
            if (user.getVerificationStatus() == VerificationStatus.PENDING) {
                return new LoginResult(AuthStatus.PENDING_VERIFICATION, user, "Your account is currently pending administrator verification.");
            } else if (user.getVerificationStatus() == VerificationStatus.REJECTED) {
                return new LoginResult(AuthStatus.ACCOUNT_REJECTED, user, "Your registration was rejected by the administrator.");
            }
        }

        return new LoginResult(AuthStatus.SUCCESS, user, "Login successful.");
    }

    public boolean isStudentIdRegistered(String studentId) {
        return userDao.getUserById(studentId.trim()) != null;
    }

    public User registerStudent(String studentId, String fullName, String password, File idCardImage) throws IOException {
        String cleanId = validateStudentId(studentId);
        if (cleanId == null) {
            return null;
        }
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        if (password == null || password.length() < 8) {
            return null;
        }
        if (userDao.getUserById(cleanId) != null) {
            return null;
        }

        String savedImagePath = null;

        if (idCardImage != null && idCardImage.exists()) {
            if (idCardImage.length() > MAX_ID_IMAGE_BYTES) {
                return null;
            }
            String extension = ".png";
            String name = idCardImage.getName();
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex >= 0) {
                String candidate = name.substring(dotIndex).toLowerCase();
                if (candidate.equals(".jpg") || candidate.equals(".jpeg") || candidate.equals(".png")) {
                    extension = candidate;
                }
            }
            extension = extension.equals(".jpeg") ? ".jpg" : extension;

            File destDir = new File(DatabaseManager.getUploadsDir());
            if (!destDir.exists()) destDir.mkdirs();

            String uploadsRoot = destDir.getCanonicalPath() + File.separator;
            File destFile = new File(destDir, "id_" + cleanId + extension);
            if (!destFile.getCanonicalPath().startsWith(uploadsRoot)) {
                return null;
            }

            Files.copy(idCardImage.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            savedImagePath = destFile.getCanonicalPath();
        }

        User user = new User(
                cleanId,
                fullName.trim(),
                DatabaseManager.hashPassword(password),
                savedImagePath,
                Role.STUDENT,
                VerificationStatus.PENDING,
                false,
                LocalDateTime.now()
        );

        boolean created = userDao.createUser(user);
        return created ? user : null;
    }

    public static String validateStudentId(String studentId) {
        if (studentId == null) {
            return null;
        }
        String cleanId = studentId.trim();
        if (cleanId.isEmpty() || cleanId.length() > 64) {
            return null;
        }
        if (!cleanId.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")) {
            return null;
        }
        return cleanId;
    }
}
