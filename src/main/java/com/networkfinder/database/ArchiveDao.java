package com.networkfinder.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.networkfinder.model.ArchiveEntry;
import com.networkfinder.model.Role;
import com.networkfinder.model.StudentProfile;
import com.networkfinder.model.User;
import com.networkfinder.model.VerificationStatus;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ArchiveDao {
    private final DatabaseManager db;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<String>>() {}.getType();

    public ArchiveDao() {
        this.db = DatabaseManager.getInstance();
    }

    /** Copies the user row (and its profile, if present) into the archive. */
    public boolean archiveUser(String studentId) {
        String sql = """
            INSERT INTO archived_users (
                student_id, first_name, middle_initial, surname, suffix, full_name,
                password_hash, id_card_image_path, role, verification_status,
                profile_completed, created_at,
                course, year_level, section, subjects, study_habits, group_size,
                interests, other_interests, profile_snapshot, archived_at)
            SELECT u.student_id, u.first_name, u.middle_initial, u.surname, u.suffix, u.full_name,
                   u.password_hash, u.id_card_image_path, u.role, u.verification_status,
                   u.profile_completed, u.created_at,
                   p.course, p.year_level, p.section, p.subjects, p.study_habits, p.group_size,
                   p.interests, p.other_interests,
                   CASE WHEN p.student_id IS NULL THEN 0 ELSE 1 END,
                   ?
            FROM users u
            LEFT JOIN profiles p ON p.student_id = u.student_id
            WHERE u.student_id = ?;
        """;
        try (Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LocalDateTime.now().toString());
            stmt.setString(2, studentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<ArchiveEntry> getArchiveEntries() {
        List<ArchiveEntry> entries = new ArrayList<>();
        String sql = "SELECT * FROM archived_users ORDER BY archived_at DESC;";
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String archivedAt = rs.getString("archived_at");
                StudentProfile profile = rs.getInt("profile_snapshot") == 1 ? mapProfileRow(rs) : null;
                entries.add(new ArchiveEntry(mapUserRow(rs), profile, archivedAt));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entries;
    }

    /**
     * Puts an archived account back into the active users/profiles tables.
     * Fails when a live account with the same Student ID already exists.
     */
    public boolean restoreUser(String studentId, String archivedAt) {
        try (Connection conn = db.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement check = conn.prepareStatement("SELECT 1 FROM users WHERE student_id = ? LIMIT 1;")) {
                    check.setString(1, studentId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                String userSql = """
                    INSERT INTO users (student_id, first_name, middle_initial, surname, suffix, full_name,
                                       password_hash, id_card_image_path, role, verification_status,
                                       profile_completed, created_at)
                    SELECT student_id, first_name, middle_initial, surname, suffix, full_name,
                           password_hash, id_card_image_path, role, verification_status,
                           COALESCE(profile_completed, 0), COALESCE(created_at, ?)
                    FROM archived_users
                    WHERE student_id = ? AND archived_at = ?;
                """;
                try (PreparedStatement stmt = conn.prepareStatement(userSql)) {
                    stmt.setString(1, LocalDateTime.now().toString());
                    stmt.setString(2, studentId);
                    stmt.setString(3, archivedAt);
                    if (stmt.executeUpdate() <= 0) {
                        conn.rollback();
                        return false;
                    }
                }

                String profileSql = """
                    INSERT INTO profiles (student_id, course, year_level, section, subjects,
                                          study_habits, group_size, interests, other_interests)
                    SELECT student_id, course, year_level, section, subjects,
                           study_habits, group_size, interests, other_interests
                    FROM archived_users
                    WHERE student_id = ? AND archived_at = ? AND profile_snapshot = 1;
                """;
                try (PreparedStatement stmt = conn.prepareStatement(profileSql)) {
                    stmt.setString(1, studentId);
                    stmt.setString(2, archivedAt);
                    stmt.executeUpdate();
                }

                String deleteSql = "DELETE FROM archived_users WHERE student_id = ? AND archived_at = ?;";
                try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                    stmt.setString(1, studentId);
                    stmt.setString(2, archivedAt);
                    stmt.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException innerEx) {
                try {
                    conn.rollback();
                } catch (SQLException ignoredRollback) {
                }
                return false;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Removes the archived record permanently (the user row must already be gone). */
    public boolean purgeUser(String studentId, String archivedAt) {
        String sql = "DELETE FROM archived_users WHERE student_id = ? AND archived_at = ?;";
        try (Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            stmt.setString(2, archivedAt);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private User mapUserRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setStudentId(rs.getString("student_id"));
        user.setFirstName(rs.getString("first_name"));
        user.setMiddleInitial(rs.getString("middle_initial"));
        user.setSurname(rs.getString("surname"));
        user.setSuffix(rs.getString("suffix"));
        user.setFullName(rs.getString("full_name"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setIdCardImagePath(rs.getString("id_card_image_path"));
        user.setRole(Role.valueOf(rs.getString("role")));
        user.setVerificationStatus(VerificationStatus.valueOf(rs.getString("verification_status")));
        user.setProfileCompleted(rs.getInt("profile_completed") != 0);
        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            try {
                user.setCreatedAt(LocalDateTime.parse(createdAtStr));
            } catch (Exception ignored) {
                user.setCreatedAt(LocalDateTime.now());
            }
        }
        return user;
    }

    private StudentProfile mapProfileRow(ResultSet rs) throws SQLException {
        StudentProfile profile = new StudentProfile();
        profile.setStudentId(rs.getString("student_id"));
        profile.setCourse(rs.getString("course"));
        profile.setYearLevel(rs.getString("year_level"));
        profile.setSection(rs.getString("section"));
        String subjectsJson = rs.getString("subjects");
        if (subjectsJson != null && !subjectsJson.isBlank()) {
            profile.setSubjects(gson.fromJson(subjectsJson, listType));
        }
        String habitsJson = rs.getString("study_habits");
        if (habitsJson != null && !habitsJson.isBlank()) {
            profile.setStudyHabits(gson.fromJson(habitsJson, listType));
        }
        profile.setGroupSizePreference(rs.getString("group_size"));
        String interestsJson = rs.getString("interests");
        if (interestsJson != null && !interestsJson.isBlank()) {
            profile.setInterests(gson.fromJson(interestsJson, listType));
        }
        profile.setOtherInterests(rs.getString("other_interests"));
        return profile;
    }
}
