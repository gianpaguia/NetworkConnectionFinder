package com.networkfinder.service;

/**
 * Single source of truth for input rules. Used by the service layer (which is
 * the authority) and by UI screens for instant, identical inline feedback.
 * Limits exist to keep stored data well-formed and label layouts intact.
 */
public final class InputValidator {

    private static final String STUDENT_ID_PATTERN = "^[A-Za-z0-9-]+$";
    private static final String NAME_PATTERN = "^[A-Za-z .'-]+$";
    private static final String PASSWORD_DIGIT_PATTERN = ".*\\d.*";
    private static final String PASSWORD_LETTER_PATTERN = ".*[A-Za-z].*";

    private static final int MAX_ID_LENGTH = 30;
    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 60;
    private static final int MAX_SECTION_LENGTH = 30;
    private static final int MAX_COURSE_LENGTH = 40;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;

    private InputValidator() {
    }

    /** Valid: non-blank, at most 30 chars, letters/digits/hyphen only. */
    public static String checkStudentId(String rawId) {
        String id = rawId == null ? "" : rawId.trim();
        if (id.isEmpty()) {
            return "Student ID is required.";
        }
        if (id.length() > MAX_ID_LENGTH) {
            return "Student ID must be at most " + MAX_ID_LENGTH + " characters.";
        }
        if (!id.matches(STUDENT_ID_PATTERN)) {
            return "Student ID may only contain letters, numbers, and hyphens.";
        }
        return null;
    }

    /** Valid: non-blank, 2-60 chars, letters/spaces/hyphens/apostrophes/periods only. */
    public static String checkFullName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            return "Full name is required.";
        }
        if (name.length() < NAME_MIN_LENGTH) {
            return "Full name must be at least " + NAME_MIN_LENGTH + " characters.";
        }
        if (name.length() > NAME_MAX_LENGTH) {
            return "Full name must be at most " + NAME_MAX_LENGTH + " characters.";
        }
        if (!name.matches(NAME_PATTERN)) {
            return "Full name may only contain letters, spaces, hyphens, apostrophes, and periods.";
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

    public static boolean isValidFullName(String rawName) {
        return checkFullName(rawName) == null;
    }

    public static boolean isValidPassword(String rawPassword) {
        return checkPassword(rawPassword) == null;
    }
}
