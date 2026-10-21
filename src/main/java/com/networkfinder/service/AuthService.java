package com.networkfinder.service;

import com.networkfinder.database.DatabaseManager;
import com.networkfinder.database.UserDao;
import com.networkfinder.model.Role;
import com.networkfinder.model.User;
import com.networkfinder.model.VerificationStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
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
        if (studentId == null || studentId.isBlank() || password == null || password == null || password.isBlank()) {
            return new LoginResult(AuthStatus.INVALID_CREDENTIALS, null, "Please enter both ID and password.");
        }

        String cleanId = studentId.trim();

        // Passwords are edge-trimmed only: the validator never issues passwords
        // with leading/trailing spaces, so stray clipboard whitespace must not
        // lock a student out at login.
        String cleanPassword = password.trim();
        if (cleanPassword.isEmpty() || cleanId.isEmpty()) {
            return new LoginResult(AuthStatus.INVALID_CREDENTIALS, null, "Please enter both ID and password.");
        }

        User user = userDao.getUserById(cleanId);
        if (user == null) {
            // Student IDs are seeded uppercase, the admin username is lowercase:
            // tolerate case typos either way instead of failing strictly here.
            user = userDao.getUserById(cleanId.toUpperCase());
        }
        if (user == null) {
            user = userDao.getUserById(cleanId.toLowerCase());
        }
        if (user == null) {
            return new LoginResult(AuthStatus.USER_NOT_FOUND, null, "Invalid Student ID or password.");
        }

        String hashedInput = DatabaseManager.hashPassword(cleanPassword);
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
        return userDao.getUserById(InputValidator.normalizeStudentId(studentId)) != null;
    }

    private static final long MAX_ID_IMAGE_BYTES = 5L * 1024 * 1024;

    /** PNG magic bytes: 0x89 'P' 'N' 'G' 0x0D 0x0A 0x1A 0x0A */
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    /** JPEG magic bytes: 0xFF 0xD8 0xFF */
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    /**
     * Registers a new PENDING student account. The service layer is the
     * validation authority: rules from {@link InputValidator} are enforced here
     * even if a caller skips UI checks. Throws {@link ValidationException} with
     * a user-presentable message for the first violated rule.
     */
    public User registerStudent(String studentId, String firstName, String middleInitial,
                                String surname, String suffix, String password, File idCardImage)
            throws IOException, ValidationException {
        String idError = InputValidator.checkStudentId(studentId);
        if (idError != null) {
            throw new ValidationException(idError);
        }
        String firstNameError = InputValidator.checkFirstName(firstName);
        if (firstNameError != null) {
            throw new ValidationException(firstNameError);
        }
        String surnameError = InputValidator.checkSurname(surname);
        if (surnameError != null) {
            throw new ValidationException(surnameError);
        }
        String middleError = InputValidator.checkMiddleInitial(middleInitial);
        if (middleError != null) {
            throw new ValidationException(middleError);
        }
        String suffixError = InputValidator.checkSuffix(suffix);
        if (suffixError != null) {
            throw new ValidationException(suffixError);
        }
        // Validate and store the edge-trimmed password so registration and
        // login hashing always agree (login also trims; see authenticate).
        String cleanPassword = password == null ? null : password.trim();
        String passwordError = InputValidator.checkPassword(cleanPassword);
        if (passwordError != null) {
            throw new ValidationException(passwordError);
        }
        if (isStudentIdRegistered(studentId)) {
            throw new ValidationException("This Student ID is already registered.");
        }

        String cleanId = InputValidator.normalizeStudentId(studentId);
        String cleanFirst = firstName.trim();
        String cleanMiddle = middleInitial == null ? "" : middleInitial.trim();
        String cleanSurname = surname.trim();
        String cleanSuffix = suffix == null ? "" : suffix.trim();
        String savedImagePath = saveIdCardImage(cleanId, idCardImage);

        User user = new User();
        user.setStudentId(cleanId);
        user.setNames(cleanFirst, cleanMiddle, cleanSurname, cleanSuffix);
        user.setPasswordHash(DatabaseManager.hashPassword(cleanPassword));
        user.setIdCardImagePath(savedImagePath);
        user.setRole(Role.STUDENT);
        user.setVerificationStatus(VerificationStatus.PENDING);
        user.setProfileCompleted(false);
        user.setCreatedAt(LocalDateTime.now());

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

    /**
     * Validates a chosen ID-card photo against the real file content, not just
     * its name. Shared by the registration screen (instant feedback when a
     * file is picked) and {@link #registerStudent} (the submit-time authority).
     * Renamed non-images have wrong bytes and are rejected. Returns an error
     * message, or null when the file is a readable PNG/JPG under the size cap.
     */
    public static String validateIdCardImage(File imageFile) {
        if (imageFile == null) {
            return null; // optional-photo callers handle the no-selection case
        }
        if (!imageFile.exists() || !imageFile.isFile()) {
            return "The selected ID photo no longer exists. Please choose it again.";
        }
        if (imageFile.length() > MAX_ID_IMAGE_BYTES) {
            return "The ID photo is too large. Please use an image under 5 MB.";
        }
        try {
            String extension = readImageExtension(imageFile);
            return extension != null ? null : "Only PNG or JPG images are accepted.";
        } catch (IOException e) {
            return "The selected ID photo could not be read. Please choose a different file.";
        }
    }

    /** Inspects magic bytes and returns ".png"/".jpg", or null for unknown content. */
    private static String readImageExtension(File imageFile) throws IOException {
        try (InputStream in = new FileInputStream(imageFile)) {
            byte[] head = in.readNBytes(PNG_SIGNATURE.length);
            if (matchesBytes(head, PNG_SIGNATURE)) {
                return ".png";
            }
            if (matchesBytes(head, JPEG_SIGNATURE)) {
                return ".jpg";
            }
            return null;
        }
    }

    private static boolean matchesBytes(byte[] data, byte[] signature) {
        if (data == null || data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Copies the validated ID photo into the uploads directory. The saved
     * extension is decided by the detected content type, so a JPEG renamed to
     * ".png" (or vice versa) is stored with the truthful extension.
     */
    private String saveIdCardImage(String cleanId, File idCardImage) throws IOException, ValidationException {
        if (idCardImage == null) {
            return null;
        }

        String validationError = validateIdCardImage(idCardImage);
        if (validationError != null) {
            throw new ValidationException(validationError);
        }

        String extension = readImageExtension(idCardImage);

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
