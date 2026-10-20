package com.networkfinder.database;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String PROJECT_ROOT = resolveProjectRoot();
    private static final String DB_DIR = PROJECT_ROOT + File.separator + "data";
    private static final String DB_PATH = DB_DIR + File.separator + "network_finder.db";
    private static final String UPLOADS_DIR = DB_DIR + File.separator + "uploads";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    public static final String DEFAULT_DEMO_PASSWORD = "password123";

    /**
     * Resolves the project root directory by locating the compiled class file
     * and walking up the directory tree until pom.xml is found.
     * Falls back to the current working directory if not found.
     */
    private static String resolveProjectRoot() {
        try {
            URI classLocation = DatabaseManager.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI();
            File dir = new File(classLocation).getAbsoluteFile();

            // Walk up until we find pom.xml (project root)
            while (dir != null) {
                if (new File(dir, "pom.xml").exists()) {
                    return dir.getAbsolutePath();
                }
                dir = dir.getParentFile();
            }
        } catch (Exception ignored) {
        }
        // Fallback: current working directory
        return System.getProperty("user.dir");
    }

    private static DatabaseManager instance;

    private DatabaseManager() {
        initDirectories();
        initDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static String getUploadsDir() {
        return UPLOADS_DIR;
    }

    private void initDirectories() {
        File dataDir = new File(DB_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File uploadsDir = new File(UPLOADS_DIR);
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs();
        }
    }

    private void initDatabase() {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                student_id TEXT PRIMARY KEY,
                full_name TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                id_card_image_path TEXT,
                role TEXT NOT NULL,
                verification_status TEXT NOT NULL,
                profile_completed INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL
            );
        """;

        String createProfilesTable = """
            CREATE TABLE IF NOT EXISTS profiles (
                student_id TEXT PRIMARY KEY,
                course TEXT,
                year_level TEXT,
                section TEXT,
                subjects TEXT,
                study_habits TEXT,
                group_size TEXT,
                interests TEXT,
                other_interests TEXT,
                FOREIGN KEY(student_id) REFERENCES users(student_id) ON DELETE CASCADE
            );
        """;

        String createConnectionsTable = """
            CREATE TABLE IF NOT EXISTS connections (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                requester_id TEXT NOT NULL,
                receiver_id TEXT NOT NULL,
                status TEXT NOT NULL,
                created_at TEXT NOT NULL,
                UNIQUE(requester_id, receiver_id),
                FOREIGN KEY(requester_id) REFERENCES users(student_id) ON DELETE CASCADE,
                FOREIGN KEY(receiver_id) REFERENCES users(student_id) ON DELETE CASCADE
            );
        """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute(createUsersTable);
            stmt.execute(createProfilesTable);
            stmt.execute(createConnectionsTable);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite database tables", e);
        }
    }

    public static String hashPassword(String password) {
        if (password == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
