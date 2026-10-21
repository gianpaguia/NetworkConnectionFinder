package com.networkfinder.service;

/**
 * Single source of truth for input rules. Used by the service layer (which is
 * the authority) and by UI screens for instant, identical inline feedback.
 * Limits exist to keep stored data well-formed and label layouts intact.
 */
public final class InputValidator {

    private static final String STUDENT_ID_PATTERN = "^[0-9]{8}-[A-Z]$";
    private static final String PERSON_NAME_PATTERN = "^[A-Za-z]+(?: [A-Za-z]+)*$";
    private static final String SHORT_NAME_PATTERN = "^[A-Za-z]{1,2}$";
    private static final String PASSWORD_DIGIT_PATTERN = ".*\\d.*";
    private static final String PASSWORD_LETTER_PATTERN = ".*[A-Za-z].*";

    private static final int VALID_ID_LENGTH = 10;
    private static final int FIRST_NAME_MAX_LENGTH = 30;
    private static final int SURNAME_MAX_LENGTH = 40;
    private static final int MAX_SECTION_LENGTH = 30;
    private static final int MAX_COURSE_LENGTH = 40;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;

    private InputValidator() {
    }

    public static int getMaxIdLength() {
        return VALID_ID_LENGTH;
    }

    /** Normalizes a student ID: trims surrounding spaces and uppercases the letter. */
    public static String normalizeStudentId(String rawId) {
        return rawId == null ? "" : rawId.trim().toUpperCase();
    }

    public static int getMaxNameLength() {
        return SURNAME_MAX_LENGTH;
    }

    /**
     * Valid format: 00000000-A (8 digits, one dash, then one letter).
     * Typed lowercase letters are normalized to uppercase before checking.
     */
    public static String checkStudentId(String rawId) {
        String id = normalizeStudentId(rawId);
        if (id.isEmpty()) {
            return "Student ID is required.";
        }
        if (id.length() != VALID_ID_LENGTH) {
            return "Student ID must be exactly 10 characters: the format 00000000-A (8 digits, a dash, then one letter).";
        }
        if (!id.matches(STUDENT_ID_PATTERN)) {
            return "Student ID must follow the format 00000000-A (8 digits, a dash, then one letter).";
        }
        return null;
    }

    /** Valid: non-blank letters/spaces only, internal spaces, at most maxLength. */
    private static String checkPersonName(String rawName, String label, int maxLength) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            return label + " is required.";
        }
        if (name.length() > maxLength) {
            return label + " must be at most " + maxLength + " characters.";
        }
        if (!name.matches(PERSON_NAME_PATTERN)) {
            return label + " may contain letters only (spaces allowed, no numbers or symbols).";
        }
        return null;
    }

    /** Valid: non-blank letters etc., at most 30 chars. */
    public static String checkFirstName(String rawFirstName) {
        return checkPersonName(rawFirstName, "First name", FIRST_NAME_MAX_LENGTH);
    }

    /** Valid: non-blank letters etc., at most 40 chars (covers surnames like "Dela Cruz"). */
    public static String checkSurname(String rawSurname) {
        return checkPersonName(rawSurname, "Surname", SURNAME_MAX_LENGTH);
    }

    /** Optional. When given: up to two letters, e.g. "J" or "Jr" style marks. */
    public static String checkMiddleInitial(String rawMiddleInitial) {
        String middle = rawMiddleInitial == null ? "" : rawMiddleInitial.trim();
        if (middle.isEmpty()) {
            return null;
        }
        if (!middle.matches(SHORT_NAME_PATTERN)) {
            return "Middle initial must be 1-2 letters only (no numbers or symbols).";
        }
        return null;
    }

    /** Optional. When given: up to two letters only, e.g. "Jr" or "Sr". */
    public static String checkSuffix(String rawSuffix) {
        String suffix = rawSuffix == null ? "" : rawSuffix.trim();
        if (suffix.isEmpty()) {
            return null;
        }
        if (!suffix.matches(SHORT_NAME_PATTERN)) {
            return "Suffix must be 1-2 letters only (e.g. Jr, Sr).";
        }
        return null;
    }

    /**
     * Valid: 8-72 chars with at least one letter and one digit.
     * The same rule guards new passwords in registration, password change, and admin resets.
     */
    public static String checkPassword(String rawPassword) {
        String password = rawPassword == null ? "" : rawPassword;
        if (password.length() < PASSWORD_MIN_LENGTH) {
            return "Password must be at least " + PASSWORD_MIN_LENGTH + " characters.";
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return "Password must be at most " + MAX_PASSWORD_LENGTH + " characters.";
        }
        if (!password.matches(PASSWORD_LETTER_PATTERN) || !password.matches(PASSWORD_DIGIT_PATTERN)) {
            return "Password must include at least one letter and one number.";
        }
        return null;
    }

    /** Valid: non-blank, at most 30 chars (any text - section codes are free-form). */
    public static String checkSection(String rawSection) {
        String section = rawSection == null ? "" : rawSection.trim();
        if (section.isEmpty()) {
            return "Section is required.";
        }
        if (section.length() > MAX_SECTION_LENGTH) {
            return "Section must be at most " + MAX_SECTION_LENGTH + " characters.";
        }
        return null;
    }

    /** Valid: blank allowed, otherwise at most 40 chars. */
    public static String checkCourse(String rawCourse) {
        String course = rawCourse == null ? "" : rawCourse.trim();
        if (course.length() > MAX_COURSE_LENGTH) {
            return "Course must be at most " + MAX_COURSE_LENGTH + " characters.";
        }
        return null;
    }

    /** Convenience for UI screens: true when the rule passes. */
    public static boolean isValidStudentId(String rawId) {
        return checkStudentId(rawId) == null;
    }

    public static boolean isValidPassword(String rawPassword) {
        return checkPassword(rawPassword) == null;
    }
}
