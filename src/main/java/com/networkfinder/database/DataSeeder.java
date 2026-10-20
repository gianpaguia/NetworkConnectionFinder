package com.networkfinder.database;

import com.networkfinder.model.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class DataSeeder {

    public static void forceResetAndSeed() {
        DatabaseManager db = DatabaseManager.getInstance();
        try (java.sql.Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM connections;");
                stmt.execute("DELETE FROM profiles;");
                stmt.execute("DELETE FROM users;");
                conn.commit();
            } catch (java.sql.SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }

        seedIfEmpty();
    }

    public static void seedIfEmpty() {
        UserDao userDao = new UserDao();
        ProfileDao profileDao = new ProfileDao();
        ConnectionDao connDao = new ConnectionDao();

        // 1. Ensure Admin exists
        if (userDao.getUserById("admin") == null) {
            User admin = new User();
            admin.setStudentId("admin");
            admin.setNames("System", null, "Administrator", null);
            admin.setPasswordHash(DatabaseManager.hashPassword("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setVerificationStatus(VerificationStatus.APPROVED);
            admin.setProfileCompleted(true);
            admin.setCreatedAt(LocalDateTime.now());
            userDao.createUser(admin);
        }

        // 2. Ensure Sample Pending Student exists for Admin Verification Demo
        String sampleIdPath = DatabaseManager.getUploadsDir() + File.separator + "sample_id_card.png";
        ensureSampleIdImage(sampleIdPath);

        if (userDao.getUserById("2026-99999") == null) {
            User pendingStudent = new User();
            pendingStudent.setStudentId("2026-99999");
            pendingStudent.setNames("Alex", null, "Tan", null);
            pendingStudent.setPasswordHash(DatabaseManager.hashPassword("student123"));
            pendingStudent.setIdCardImagePath(sampleIdPath);
            pendingStudent.setRole(Role.STUDENT);
            pendingStudent.setVerificationStatus(VerificationStatus.PENDING);
            pendingStudent.setProfileCompleted(false);
            pendingStudent.setCreatedAt(LocalDateTime.now().minusHours(2));
            userDao.createUser(pendingStudent);
        }

        // 3. Ensure Verified Students Network exists
        if (userDao.getUserById("2026-10001") == null) {
            seedStudents(userDao, profileDao, connDao);
        }
    }

    private static void seedStudents(UserDao userDao, ProfileDao profileDao, ConnectionDao connDao) {
        String defaultPass = DatabaseManager.hashPassword(DatabaseManager.DEFAULT_DEMO_PASSWORD);

        // Student 1: Juan Dela Cruz
        createStudent(userDao, profileDao,
                "2026-10001", "Juan Dela Cruz", defaultPass,
                "Bachelor of Science in Information Technology", "1st Year", "BSIT 1A",
                List.of("Programming", "Data Structures", "Networking"),
                List.of("Coding Together", "Group Discussions", "Working on Projects"),
                "Small Group",
                List.of("Technology", "Gaming"), "Robotics");

        // Student 2: Maria Santos
        createStudent(userDao, profileDao,
                "2026-10002", "Maria Santos", defaultPass,
                "Bachelor of Science in Information Technology", "1st Year", "BSIT 1A",
                List.of("Programming", "Database Management", "Web Development"),
                List.of("Coding Together", "Reviewing Lessons", "Sharing Notes"),
                "Small Group",
                List.of("Technology", "Arts"), "UI/UX Design");

        // Student 3: Mark Reyes
        createStudent(userDao, profileDao,
                "2026-10003", "Mark Reyes", defaultPass,
                "Bachelor of Science in Information Technology", "1st Year", "BSIT 1B",
                List.of("Data Structures", "Mobile Development", "Cybersecurity"),
                List.of("Coding Together", "Solving Problems"),
                "Medium Group",
                List.of("Gaming", "Fitness"), "App Dev");

        // Student 4: Patricia Gomez
        createStudent(userDao, profileDao,
                "2026-10004", "Patricia Gomez", defaultPass,
                "Bachelor of Science in Computer Science", "1st Year", "BSCS 1A",
                List.of("Programming", "Mathematics", "Software Engineering"),
                List.of("Solving Problems", "Exam Review"),
                "Small Group",
                List.of("Music", "Academic Organizations"), "Algorithms");

        // Student 5: Christian Lim
        createStudent(userDao, profileDao,
                "2026-10005", "Christian Lim", defaultPass,
                "Bachelor of Science in Computer Science", "1st Year", "BSCS 1A",
                List.of("Mathematics", "Data Structures", "Research"),
                List.of("Reviewing Lessons", "Exam Review"),
                "Medium Group",
                List.of("Research", "Student Activities"), "Competitive Programming");

        // Student 6: Bea Alonzo
        createStudent(userDao, profileDao,
                "2026-10006", "Bea Alonzo", defaultPass,
                "Bachelor of Science in Information Technology", "2nd Year", "BSIT 2A",
                List.of("Web Development", "Database Management", "Software Engineering"),
                List.of("Group Discussions", "Working on Projects"),
                "Large Group",
                List.of("Photography", "Volunteering"), "Web Services");

        // Student 7: Joshua Garcia
        createStudent(userDao, profileDao,
                "2026-10007", "Joshua Garcia", defaultPass,
                "Bachelor of Science in Information Technology", "2nd Year", "BSIT 2A",
                List.of("Mobile Development", "Networking", "Cybersecurity"),
                List.of("Coding Together", "Solving Problems"),
                "Small Group",
                List.of("Sports", "Fitness"), "Network Sec");

        // Student 8: Angel Locsin
        createStudent(userDao, profileDao,
                "2026-10008", "Angel Locsin", defaultPass,
                "Bachelor of Science in Computer Science", "2nd Year", "BSCS 2A",
                List.of("Software Engineering", "Research", "Programming"),
                List.of("Group Discussions", "Sharing Notes"),
                "Medium Group",
                List.of("Volunteering", "Academic Organizations"), "Open Source");

        // Student 9: Daniel Padilla
        createStudent(userDao, profileDao,
                "2026-10009", "Daniel Padilla", defaultPass,
                "Bachelor of Science in Computer Science", "2nd Year", "BSCS 2A",
                List.of("Mathematics", "Cybersecurity", "Networking"),
                List.of("Solving Problems", "Working on Projects"),
                "Small Group",
                List.of("Music", "Gaming"), "Cyber Labs");

        // Student 10: Kathryn Bernardo
        createStudent(userDao, profileDao,
                "2026-10010", "Kathryn Bernardo", defaultPass,
                "Bachelor of Science in Information Technology", "1st Year", "BSIT 1B",
                List.of("Data Structures", "Web Development", "Communication"),
                List.of("Reviewing Lessons", "Sharing Notes"),
                "Small Group",
                List.of("Arts", "Student Activities"), "Creative Writing");

        // 4. Seed Network Connections (Edges in Graph)
        // Juan is connected to Maria (1st degree) and Kathryn (1st degree)
        connDao.addConnection("2026-10001", "2026-10002", ConnectionStatus.ACCEPTED);
        connDao.addConnection("2026-10001", "2026-10010", ConnectionStatus.ACCEPTED);

        // Maria is connected to Mark and Patricia
        connDao.addConnection("2026-10002", "2026-10003", ConnectionStatus.ACCEPTED);
        connDao.addConnection("2026-10002", "2026-10004", ConnectionStatus.ACCEPTED);

        // Mark is connected to Joshua and Bea
        connDao.addConnection("2026-10003", "2026-10007", ConnectionStatus.ACCEPTED);
        connDao.addConnection("2026-10003", "2026-10006", ConnectionStatus.ACCEPTED);

        // Patricia is connected to Christian and Angel
        connDao.addConnection("2026-10004", "2026-10005", ConnectionStatus.ACCEPTED);
        connDao.addConnection("2026-10004", "2026-10008", ConnectionStatus.ACCEPTED);

        // Christian is connected to Daniel
        connDao.addConnection("2026-10005", "2026-10009", ConnectionStatus.ACCEPTED);

        // Bea is connected to Joshua
        connDao.addConnection("2026-10006", "2026-10007", ConnectionStatus.ACCEPTED);

        // Angel is connected to Daniel
        connDao.addConnection("2026-10008", "2026-10009", ConnectionStatus.ACCEPTED);
    }

    private static void createStudent(UserDao userDao, ProfileDao profileDao,
                                      String id, String name, String pass,
                                      String course, String year, String section,
                                      List<String> subjects, List<String> habits,
                                      String groupSize, List<String> interests, String other) {
        if (userDao.getUserById(id) == null) {
            User user = new User();
            user.setStudentId(id);
            // Benchmark seed names: first token is the given name, the rest is the surname.
            String trimmed = name.trim();
            int spaceIndex = trimmed.indexOf(' ');
            if (spaceIndex > 0) {
                user.setNames(trimmed.substring(0, spaceIndex), null, trimmed.substring(spaceIndex + 1), null);
            } else {
                user.setNames(trimmed, null, "", null);
            }
            user.setPasswordHash(pass);
            user.setRole(Role.STUDENT);
            user.setVerificationStatus(VerificationStatus.APPROVED);
            user.setProfileCompleted(true);
            user.setCreatedAt(LocalDateTime.now());
            userDao.createUser(user);
        }

        StudentProfile profile = new StudentProfile(id, course, year, section, subjects, habits, groupSize, interests, other);
        profileDao.saveOrUpdateProfile(profile);
    }

    private static void ensureSampleIdImage(String path) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                BufferedImage image = new BufferedImage(480, 280, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background
                g.setColor(new Color(245, 247, 250));
                g.fillRect(0, 0, 480, 280);

                // Card border
                g.setColor(new Color(30, 64, 175));
                g.setStroke(new BasicStroke(4));
                g.drawRoundRect(10, 10, 460, 260, 16, 16);

                // Header band
                g.setColor(new Color(30, 64, 175));
                g.fillRect(10, 10, 460, 50);

                // Header text
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 18));
                g.drawString("UNIVERSITY STUDENT IDENTIFICATION", 30, 42);

                // Photo placeholder box
                g.setColor(new Color(226, 232, 240));
                g.fillRoundRect(30, 80, 120, 140, 8, 8);
                g.setColor(new Color(148, 163, 184));
                g.drawRoundRect(30, 80, 120, 140, 8, 8);
                g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g.drawString("[PHOTO]", 68, 155);

                // ID Details
                g.setColor(new Color(15, 23, 42));
                g.setFont(new Font("SansSerif", Font.BOLD, 16));
                g.drawString("ALEX TAN", 170, 110);

                g.setFont(new Font("SansSerif", Font.PLAIN, 13));
                g.setColor(new Color(71, 85, 105));
                g.drawString("Student ID: 2026-99999", 170, 135);
                g.drawString("Course: BS Information Tech", 170, 160);
                g.drawString("Academic Year: 2026-2027", 170, 185);
                g.drawString("Status: Regular Student", 170, 210);

                // Barcode simulation
                g.setColor(new Color(30, 41, 59));
                for (int x = 170; x < 440; x += 6) {
                    g.fillRect(x, 225, (x % 4 == 0) ? 3 : 2, 22);
                }

                g.dispose();
                ImageIO.write(image, "png", file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
