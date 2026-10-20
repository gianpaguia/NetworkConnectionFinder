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

        String hashedInput = DatabaseManager.hashPassword(password);
        if (!hashedInput.equals(user.getPasswordHash())) {
            return new LoginResult(AuthStatus.INVALID_CREDENTIALS, null, "Incorrect password.");
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
        String cleanId = studentId.trim();
        String savedImagePath = null;

        if (idCardImage != null && idCardImage.exists()) {
            String extension = "";
            String name = idCardImage.getName();
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = name.substring(dotIndex);
            }
            File destDir = new File(DatabaseManager.getUploadsDir());
            if (!destDir.exists()) destDir.mkdirs();

            File destFile = new File(destDir, "id_" + cleanId.replace("-", "_") + extension);
            Files.copy(idCardImage.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            savedImagePath = destFile.getPath();
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
}
