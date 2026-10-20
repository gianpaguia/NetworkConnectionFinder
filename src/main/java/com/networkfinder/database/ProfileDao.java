package com.networkfinder.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.networkfinder.model.StudentProfile;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProfileDao {
    private final DatabaseManager db;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<String>>() {}.getType();

    public ProfileDao() {
        this.db = DatabaseManager.getInstance();
    }

    public boolean saveOrUpdateProfile(StudentProfile profile) {
        try (Connection conn = db.getConnection()) {
            return saveOrUpdateProfile(profile, conn);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveOrUpdateProfile(StudentProfile profile, Connection conn) {
        String sql = """
            INSERT INTO profiles (student_id, course, year_level, section, subjects, study_habits, group_size, interests, other_interests)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(student_id) DO UPDATE SET
                course = excluded.course,
                year_level = excluded.year_level,
                section = excluded.section,
                subjects = excluded.subjects,
                study_habits = excluded.study_habits,
                group_size = excluded.group_size,
                interests = excluded.interests,
                other_interests = excluded.other_interests;
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, profile.getStudentId());
            stmt.setString(2, profile.getCourse());
            stmt.setString(3, profile.getYearLevel());
            stmt.setString(4, profile.getSection());
            stmt.setString(5, gson.toJson(profile.getSubjects()));
            stmt.setString(6, gson.toJson(profile.getStudyHabits()));
            stmt.setString(7, profile.getGroupSizePreference());
            stmt.setString(8, gson.toJson(profile.getInterests()));
            stmt.setString(9, profile.getOtherInterests());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public StudentProfile getProfile(String studentId) {
        String sql = "SELECT * FROM profiles WHERE student_id = ?;";
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

    public List<StudentProfile> getAllProfiles() {
        List<StudentProfile> list = new ArrayList<>();
        String sql = "SELECT * FROM profiles;";
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean deleteProfile(String studentId) {
        String sql = "DELETE FROM profiles WHERE student_id = ?;";
        try (Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private StudentProfile mapRow(ResultSet rs) throws SQLException {
        StudentProfile profile = new StudentProfile();
        profile.setStudentId(rs.getString("student_id"));
        profile.setCourse(rs.getString("course"));
        profile.setYearLevel(rs.getString("year_level"));
        profile.setSection(rs.getString("section"));

        String subjectsJson = rs.getString("subjects");
        if (subjectsJson != null && !subjectsJson.isBlank()) {
            List<String> list = gson.fromJson(subjectsJson, listType);
            profile.setSubjects(list);
        }

        String habitsJson = rs.getString("study_habits");
        if (habitsJson != null && !habitsJson.isBlank()) {
            List<String> list = gson.fromJson(habitsJson, listType);
            profile.setStudyHabits(list);
        }

        profile.setGroupSizePreference(rs.getString("group_size"));

        String interestsJson = rs.getString("interests");
        if (interestsJson != null && !interestsJson.isBlank()) {
            List<String> list = gson.fromJson(interestsJson, listType);
            profile.setInterests(list);
        }

        profile.setOtherInterests(rs.getString("other_interests"));
        return profile;
    }
}
