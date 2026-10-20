package com.networkfinder.service;

import com.networkfinder.database.DatabaseManager;
import com.networkfinder.database.UserDao;
import com.networkfinder.model.Role;
import com.networkfinder.model.User;
import com.networkfinder.model.VerificationStatus;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Set;

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
            return new LoginResult(AuthStatus.USER_NOT_FOUND, null, "Invalid Student ID or password.");
        }

        String hashedInput = DatabaseManager.hashPassword(password);
        if (!constantTimeEquals(hashedInput, user.getPasswordHash())) {
            return new LoginResult(AuthStatus.INVALID_CREDENTIALS, null, "Invalid Student ID or password.");
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

    private static final Set<String> ALLOWED_ID_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg");

    /**
     * Registers a new PENDING student account. The service layer is the
     * validation authority: rules from {@link InputValidator} are enforced here
     * even if a caller skips UI checks. Throws {@link ValidationException} with
     * a user-presentable message for the first violated rule.
     */
    public User registerStudent(String studentId, String fullName, String password, File idCardImage)
            throws IOException, ValidationException {
        String idError = InputValidator.checkStudentId(studentId);
        if (idError != null) {
            throw new ValidationException(idError);
        }
        String nameError = InputValidator.checkFullName(fullName);
        if (nameError != null) {
            throw new ValidationException(nameError);
        }
        String passwordError = InputValidator.checkPassword(password);
        if (passwordError != null) {
            throw new ValidationException(passwordError);
        }
        if (isStudentIdRegistered(studentId)) {
            throw new ValidationException("This Student ID is already registered.");
        }

        String cleanId = studentId.trim();
        String savedImagePath = saveIdCardImage(cleanId, idCardImage);

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
        if (!created) {
            // createUser only fails on SQLException; the id-card upload above
            // succeeded, so nothing else was persisted. Nothing to clean up.
            return null;
        }
        return user;
    }

    /** Result of a password change attempt. */
    public enum PasswordChangeResult {
        SUCCESS,
        WRONG_CURRENT_PASSWORD,
        UNKNOWN_USER,
        ERROR
    }

    /**
     * Changes a user's password after verifying the current one. The service
     * re-validates the new password regardless of what the UI allowed.
     * Admin accounts can only change passwords through their own process.
     */
    public PasswordChangeResult changePassword(String studentId, String currentPassword, String newPassword)
            throws ValidationException {
        String passwordError = InputValidator.checkPassword(newPassword);
        if (passwordError != null) {
            throw new ValidationException(passwordError);
        }
        User user = userDao.getUserById(studentId == null ? "" : studentId.trim());
        if (user == null) {
            return PasswordChangeResult.UNKNOWN_USER;
        }
        if (user.getRole() == Role.ADMIN) {
            return PasswordChangeResult.ERROR;
        }
        String hashedCurrent = DatabaseManager.hashPassword(currentPassword == null ? "" : currentPassword);
        if (!constantTimeEquals(hashedCurrent, user.getPasswordHash())) {
            return PasswordChangeResult.WRONG_CURRENT_PASSWORD;
        }
        boolean updated = userDao.updatePasswordHash(user.getStudentId(), DatabaseManager.hashPassword(newPassword));
        return updated ? PasswordChangeResult.SUCCESS : PasswordChangeResult.ERROR;
    }

    private String saveIdCardImage(String cleanId, File idCardImage) throws IOException {
        if (idCardImage == null || !idCardImage.exists()) {
            return null;
        }

        String name = idCardImage.getName();
        String extension = "";
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = name.substring(dotIndex).toLowerCase();
        }
        if (!ALLOWED_ID_EXTENSIONS.contains(extension)) {
            throw new IOException("Only PNG, JPG, or JPEG ID card images are accepted.");
        }

        String safeBase = cleanId.replaceAll("[^A-Za-z0-9_-]", "_");
        File destDir = new File(DatabaseManager.getUploadsDir());
        if (!destDir.exists()) destDir.mkdirs();

        File destFile = new File(destDir, "id_" + safeBase + extension);
        Path uploadsRoot = Paths.get(destDir.getCanonicalPath());
        Path target = Paths.get(destFile.getCanonicalPath());
        if (!target.startsWith(uploadsRoot)) {
            throw new IOException("Invalid upload destination path.");
        }

        Files.copy(idCardImage.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return destFile.getPath();
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }
}
