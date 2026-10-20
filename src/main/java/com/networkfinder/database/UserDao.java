package com.networkfinder.database;

import com.networkfinder.model.Role;
import com.networkfinder.model.User;
import com.networkfinder.model.VerificationStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private final DatabaseManager db;

    public UserDao() {
        this.db = DatabaseManager.getInstance();
    }

    public boolean createUser(User user) {
        String sql = """
            INSERT INTO users (student_id, full_name, password_hash, id_card_image_path, role, verification_status, profile_completed, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getStudentId());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getIdCardImagePath());
            stmt.setString(5, user.getRole().name());
            stmt.setString(6, user.getVerificationStatus().name());
            stmt.setInt(7, user.isProfileCompleted() ? 1 : 0);
            stmt.setString(8, user.getCreatedAt() != null ? user.getCreatedAt().toString() : LocalDateTime.now().toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public User getUserById(String studentId) {
        String sql = "SELECT * FROM users WHERE student_id = ?;";
        try (Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<User> getPendingStudents() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'STUDENT' AND verification_status = 'PENDING' ORDER BY created_at ASC;";
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<User> getApprovedStudents() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'STUDENT' AND verification_status = 'APPROVED' ORDER BY student_id ASC;";
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY student_id ASC;";
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateVerificationStatus(String studentId, VerificationStatus status) {
        String sql = "UPDATE users SET verification_status = ? WHERE student_id = ?;";
        try (Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, studentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateProfileCompleted(String studentId, boolean completed) {
        String sql = "UPDATE users SET profile_completed = ? WHERE student_id = ?;";
        try (Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, completed ? 1 : 0);
            stmt.setString(2, studentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateStudentInfo(String studentId, String fullName) {
        String sql = "UPDATE users SET full_name = ? WHERE student_id = ?;";
        try (Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fullName);
            stmt.setString(2, studentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePasswordHash(String studentId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE student_id = ?;";
        try (Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passwordHash);
            stmt.setString(2, studentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser(String studentId) {
        String sql = "DELETE FROM users WHERE student_id = ?;";
        try (Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setStudentId(rs.getString("student_id"));
        user.setFullName(rs.getString("full_name"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setIdCardImagePath(rs.getString("id_card_image_path"));
        user.setRole(parseRole(rs.getString("role")));
        user.setVerificationStatus(parseVerificationStatus(rs.getString("verification_status")));
        user.setProfileCompleted(rs.getInt("profile_completed") == 1);
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

    private Role parseRole(String value) {
        try {
            return value == null ? Role.STUDENT : Role.valueOf(value);
        } catch (IllegalArgumentException e) {
            return Role.STUDENT;
        }
    }

    private VerificationStatus parseVerificationStatus(String value) {
        try {
            return value == null ? VerificationStatus.REJECTED : VerificationStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return VerificationStatus.REJECTED;
        }
    }
}
