package com.networkfinder.database;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class DatabaseManager {
    private static final String LEGACY_DB_PATH = resolveProjectRoot() + File.separator + "data"
            + File.separator + "network_finder.db";
    private static final String DB_DIR = resolveProjectRoot() + File.separator + "data";
    private static final String DB_PATH = resolveDbPath();
    private static final String UPLOADS_DIR = DB_DIR + File.separator + "uploads";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    private static final String HASH_PREFIX = "pbkdf2$";
    private static final int HASH_ITERATIONS = 120_000;
    private static final int HASH_SALT_BYTES = 16;
    private static final int HASH_KEY_BITS = 256;
    private static final SecureRandom HASH_RANDOM = new SecureRandom();

    /**
     * Allows tests to redirect the SQLite file via the
     * "networkfinder.db.path" system property (set before getInstance()).
     */
    private static String resolveDbPath() {
        String override = System.getProperty("networkfinder.db.path");
        if (override != null && !override.isBlank()) {
            return override;
        }
        return LEGACY_DB_PATH;
    }

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
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement pragma = conn.createStatement()) {
            pragma.execute("PRAGMA foreign_keys = ON;");
            pragma.execute("PRAGMA busy_timeout = 5000;");
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
        return conn;
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

        File dbFile = new File(DB_PATH);
        if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
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

    /**
     * Hashes a password with PBKDF2-HMAC-SHA256 and a per-user random salt.
     * Format: "pbkdf2$<iterations>$<base64 salt>$<base64 key>"
     */
    public static String hashPassword(String password) {
        if (password == null) return "";
        byte[] salt = new byte[HASH_SALT_BYTES];
        HASH_RANDOM.nextBytes(salt);
        byte[] key = pbkdf2(password, salt, HASH_ITERATIONS);
        return HASH_PREFIX + HASH_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(key);
    }

    /**
     * Verifies a password against a stored hash. Supports both the current
     * PBKDF2 format and the legacy unsalted SHA-256 hex format.
     */
    public static boolean verifyPassword(String password, String stored) {
        if (password == null || stored == null) return false;
        if (stored.startsWith(HASH_PREFIX)) {
            try {
                String[] parts = stored.split("\\$");
                if (parts.length != 4) return false;
                int iterations = Integer.parseInt(parts[1]);
                if (iterations < 1) return false;
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);
                byte[] actual = pbkdf2(password, salt, iterations);
                return MessageDigest.isEqual(expected, actual);
            } catch (RuntimeException e) {
                return false;
            }
        }
        // Legacy unsalted SHA-256 (pre-migration rows)
        return legacySha256Hex(password).equals(stored);
    }

    /**
     * True when the stored hash predates the PBKDF2 format and should be
     * upgraded the next time the correct password is supplied.
     */
    public static boolean isLegacyHash(String stored) {
        return stored != null && !stored.isBlank() && !stored.startsWith(HASH_PREFIX);
    }

    public static String legacySha256Hex(String input) {
        if (input == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
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

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException e) {
            throw new RuntimeException("PBKDF2 algorithm not available", e);
        } finally {
            spec.clearPassword();
        }
    }
}
