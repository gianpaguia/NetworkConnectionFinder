package com.networkfinder.service;

import com.networkfinder.database.DataSeeder;
import com.networkfinder.database.DatabaseManager;
import com.networkfinder.database.UserDao;
import com.networkfinder.dsa.MatchFilter;
import com.networkfinder.dsa.MatchResult;
import com.networkfinder.dsa.PathResult;
import com.networkfinder.model.ArchiveEntry;
import com.networkfinder.model.Role;
import com.networkfinder.model.User;
import com.networkfinder.model.VerificationStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceIntegrationTest {

    static {
        try {
            // Point the whole test suite at a disposable database so test runs
            // (including the destructive adminResetDataset test below) NEVER
            // wipe the real project database in data/network_finder.db where
            // manually created accounts live.
            Path testDb = Files.createTempFile("networkfinder-test-db-", ".db");
            System.setProperty(DatabaseManager.DB_PATH_PROPERTY, testDb.toAbsolutePath().toString());
            testDb.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare the isolated test database", e);
        }
    }

    private static final java.util.concurrent.atomic.AtomicLong TEST_SEQ = new java.util.concurrent.atomic.AtomicLong();

    /**
     * Unique, rule-compliant student ID for each test run (leaves the benchmark
     * dataset untouched). Format: 00000000-A — 8 digits plus one dashed letter.
     */
    private static String uniqueTestId() {
        long seq = TEST_SEQ.incrementAndGet();
        long digits = 10000000L + ((System.currentTimeMillis() * 31 + seq * 7919L) % 89999999L);
        char letter = (char) ('A' + (int) (seq % 26));
        return String.format("%08d", digits) + "-" + letter;
    }

    @BeforeAll
    public static void setUp() {
        DatabaseManager.getInstance();
        DataSeeder.seedIfEmpty();
        NetworkService.getInstance().reloadGraph();
    }

    @Test
    public void testAdminSeededAndAuthenticates() {
        AuthService authService = new AuthService();
        AuthService.LoginResult res = authService.authenticate("admin", "admin123");
        assertEquals(AuthService.AuthStatus.SUCCESS, res.getStatus());
        assertNotNull(res.getUser());
        assertEquals(Role.ADMIN, res.getUser().getRole());
    }

    @Test
    public void testStudentRegistrationAndVerificationWorkflow() throws Exception {
        AuthService authService = new AuthService();
        VerificationService verificationService = new VerificationService();

        String testId = uniqueTestId();
        // 1. Register student
        User student = authService.registerStudent(testId, "Test", null, "Student", null, "pass1234", null);
        assertNotNull(student);
        assertEquals(VerificationStatus.PENDING, student.getVerificationStatus());

        // 2. Student cannot login while pending
        AuthService.LoginResult loginPending = authService.authenticate(testId, "pass1234");
        assertEquals(AuthService.AuthStatus.PENDING_VERIFICATION, loginPending.getStatus());

        // 3. Admin sees pending student in list
        List<User> pending = verificationService.getPendingVerifications();
        assertTrue(pending.stream().anyMatch(u -> u.getStudentId().equals(testId)));

        // 4. Admin approves student
        assertTrue(verificationService.approveStudent(testId));

        // 5. Student can now login
        AuthService.LoginResult loginApproved = authService.authenticate(testId, "pass1234");
        assertEquals(AuthService.AuthStatus.SUCCESS, loginApproved.getStatus());
    }

    @Test
    public void testNetworkServiceGraphBFSOnSeededData() {
        NetworkService netService = NetworkService.getInstance();
        netService.reloadGraph();

        // Check seeded student Juan Dela Cruz (20260001-A) to Daniel Padilla (20260009-I)
        // Expected route: Juan -> Maria -> Patricia -> Christian -> Daniel (or via Angel)
        PathResult pathResult = netService.findShortestPath("20260001-A", "20260009-I");
        assertTrue(pathResult.isReachable());
        assertTrue(pathResult.getDegreesOfSeparation() >= 3);
        assertTrue(pathResult.getPath().contains("20260001-A"));
        assertTrue(pathResult.getPath().contains("20260009-I"));
    }

    @Test
    public void testStudyGroupMatcherLevelOrderOnSeededData() {
        NetworkService netService = NetworkService.getInstance();
        netService.reloadGraph();

        // Search for Programming peers from Juan Dela Cruz (20260001-A)
        MatchFilter filter = new MatchFilter().setRequiredSubject("Programming");
        List<MatchResult> matches = netService.matchStudyGroups("20260001-A", filter);

        assertFalse(matches.isEmpty());
        // Closest match should be 1st degree peer Maria Santos
        assertEquals("20260002-B", matches.get(0).getStudent().getStudentId());
        assertEquals(1, matches.get(0).getDegreeOfSeparation());
    }

    @Test
    public void testMutualConnectionsOnSeededData() {
        NetworkService netService = NetworkService.getInstance();
        netService.reloadGraph();

        // Both Bea (20260006-F) and Joshua (20260007-G) connect to Mark (20260003-C)
        List<String> mutuals = netService.getMutualConnections("20260006-F", "20260007-G");
        assertTrue(mutuals.contains("20260003-C"));
    }

    @Test
    public void testTwoWayConnectionRequestWorkflow() {
        NetworkService netService = NetworkService.getInstance();
        netService.reloadGraph();

        String s1 = "20260001-A"; // Juan
        String s7 = "20260007-G"; // Joshua (not directly connected to Juan initially)

        // Make sure no direct connection initially
        netService.removeConnection(s1, s7);
        assertFalse(netService.hasConnection(s1, s7));

        // 1. Juan sends connection request to Joshua
        assertTrue(netService.sendConnectionRequest(s1, s7));
        assertFalse(netService.hasConnection(s1, s7)); // Not connected until accepted!

        // 2. Joshua sees incoming request from Juan
        List<com.networkfinder.model.Connection> incoming = netService.getIncomingRequests(s7);
        assertTrue(incoming.stream().anyMatch(c -> c.getRequesterId().equals(s1)));

        // 3. Joshua accepts connection request
        assertTrue(netService.acceptConnectionRequest(s1, s7));
        assertTrue(netService.hasConnection(s1, s7)); // Now active edge in graph!
        assertTrue(netService.getGraph().hasEdge(s1, s7));

        // Shortest path between Juan and Joshua is now 1 direct hop
        PathResult path = netService.findShortestPath(s1, s7);
        assertTrue(path.isReachable());
        assertEquals(1, path.getDegreesOfSeparation());

        // 4. Cleanup
        assertTrue(netService.removeConnection(s1, s7));
        assertFalse(netService.hasConnection(s1, s7));
    }

    @Test
    public void testAdminDataManagementAndReset() throws Exception {
        NetworkService netService = NetworkService.getInstance();

        // 1. Admin adds student
        String testStudentId = uniqueTestId();
        assertTrue(netService.adminAddStudent(testStudentId, "Admin", null, "Added Student", null, "BSIT", "1st Year", "BSIT 1A"));
        assertTrue(netService.getGraph().containsVertex(testStudentId));
        assertEquals("Admin Added Student", netService.getGraph().getNode(testStudentId).getFullName());

        // 2. Admin edits student
        assertTrue(netService.adminEditStudent(testStudentId, "Renamed", null, "Student", null, "BSCS", "2nd Year", "BSCS 2A"));
        assertEquals("Renamed Student", netService.getGraph().getNode(testStudentId).getFullName());
        assertEquals("BSCS 2A", netService.getGraph().getNode(testStudentId).getProfile().getSection());

        // 3. Admin adds connection between test student and Juan
        assertTrue(netService.addConnection(testStudentId, "20260001-A"));
        assertTrue(netService.hasConnection(testStudentId, "20260001-A"));

        // 4. Admin deletes student (kept in archive, restorable)
        assertTrue(netService.adminDeleteStudent(testStudentId));
        assertFalse(netService.getGraph().containsVertex(testStudentId));
        assertFalse(netService.hasConnection(testStudentId, "20260001-A"));
        assertFalse(netService.isStudentIdInUse(testStudentId));
        assertTrue(netService.getArchivedStudents().stream()
                .anyMatch(e -> testStudentId.equals(e.getUser().getStudentId())));

        // 5. Admin resets dataset to initial benchmark state
        netService.adminResetDataset();
        assertEquals(10, netService.getGraph().getVertexCount());
        assertEquals(11, netService.getGraph().getEdgeCount());
    }

    @Test
    public void testAdminArchiveRestoreAndPurgeWorkflow() throws Exception {
        NetworkService netService = NetworkService.getInstance();

        String testId = uniqueTestId();

        // 1. Admin creates a student with a profile, then deletes it
        assertTrue(netService.adminAddStudent(testId, "Archive", null, "Restore", null, "BSIT", "3rd Year", "BSIT 3B"));
        assertTrue(netService.adminDeleteStudent(testId));
        assertFalse(netService.isStudentIdInUse(testId));
        assertFalse(netService.getGraph().containsVertex(testId));

        ArchiveEntry original = netService.getArchivedStudents().stream()
                .filter(e -> testId.equals(e.getUser().getStudentId()))
                .findFirst()
                .orElse(null);
        assertNotNull(original);
        assertNotNull(original.getProfile());
        assertEquals("Archive Restore", original.getUser().getFullName());
        assertEquals("BSIT", original.getProfile().getCourse());
        assertEquals("BSIT 3B", original.getProfile().getSection());
        assertNotNull(original.getArchivedAt());

        // 2. Restore is refused while a live account occupies the same Student ID
        assertTrue(netService.adminAddStudent(testId, "Duplicate", null, "Occupant", null, "BSCS", "1st Year", "BSCS 1A"));
        assertFalse(netService.restoreArchivedStudent(testId, original.getArchivedAt()));

        // 3. Deleting the occupant archives a second snapshot of the same ID
        assertTrue(netService.adminDeleteStudent(testId));
        assertEquals(2, netService.getArchivedStudents().stream()
                .filter(e -> testId.equals(e.getUser().getStudentId()))
                .count());

        // 4. Restoring by the original archive timestamp brings back the original snapshot
        assertTrue(netService.restoreArchivedStudent(testId, original.getArchivedAt()));
        assertTrue(netService.isStudentIdInUse(testId));
        assertTrue(netService.getGraph().containsVertex(testId));
        assertEquals("Archive Restore", netService.getGraph().getNode(testId).getFullName());
        assertEquals("BSIT 3B", netService.getGraph().getNode(testId).getProfile().getSection());

        // 5. The foreign snapshot of the same ID is untouched, purge removes it permanently
        ArchiveEntry leftover = netService.getArchivedStudents().stream()
                .filter(e -> testId.equals(e.getUser().getStudentId()))
                .findFirst()
                .orElse(null);
        assertNotNull(leftover);
        assertTrue(netService.purgeArchivedStudent(testId, leftover.getArchivedAt()));
        assertFalse(netService.getArchivedStudents().stream()
                .anyMatch(e -> testId.equals(e.getUser().getStudentId())));
        assertTrue(netService.isStudentIdInUse(testId));
    }

    // ==================================================
    // VALIDATION AUTHORITY (service rejects bad input)
    // ==================================================

    @Test
    public void testRegisterValidationRejectsBadInput() throws Exception {
        AuthService authService = new AuthService();

        // Student ID containing letters, symbols, and spaces (00000000-A rule)
        assertThrows(ValidationException.class, () ->
                authService.registerStudent("bad id!", "Valid", null, "Name", null, "pass1234", null));
        assertThrows(ValidationException.class, () ->
                authService.registerStudent("2026AB12", "Valid", null, "Name", null, "pass1234", null));

        // Password below the minimum length
        assertThrows(ValidationException.class, () ->
                authService.registerStudent(uniqueTestId(), "Valid", null, "Name", null, "short1", null));

        // Password without a digit
        assertThrows(ValidationException.class, () ->
                authService.registerStudent(uniqueTestId(), "Valid", null, "Name", null, "nodigitpass", null));

        // Blank first name
        assertThrows(ValidationException.class, () ->
                authService.registerStudent(uniqueTestId(), "   ", null, "Tan", null, "pass1234", null));

        // First name containing digits
        assertThrows(ValidationException.class, () ->
                authService.registerStudent(uniqueTestId(), "Juan123", null, "Tan", null, "pass1234", null));

        // Middle initial with punctuation (periods no longer accepted)
        assertThrows(ValidationException.class, () ->
                authService.registerStudent(uniqueTestId(), "Juan", "M.", "Tan", null, "pass1234", null));

        // Suffix containing digits
        assertThrows(ValidationException.class, () ->
                authService.registerStudent(uniqueTestId(), "Juan", null, "Tan", "2nd", "pass1234", null));

        // Duplicate Student ID
        String duplicateId = uniqueTestId();
        authService.registerStudent(duplicateId, "First", null, "User", null, "pass1234", null);
        assertThrows(ValidationException.class, () ->
                authService.registerStudent(duplicateId, "Second", null, "User", null, "pass1234", null));
    }

    // ==================================================
    // STUDENT ID FORMAT (00000000-A) & CASE NORMALIZATION
    // ==================================================

    @Test
    public void testStudentIdFormatEnforcementAndNormalization() throws Exception {
        AuthService authService = new AuthService();

        // 7 digits rejected
        assertThrows(ValidationException.class, () ->
                authService.registerStudent("1234567-A", "Format", null, "Check", null, "pass1234", null));
        // 9 digits rejected
        assertThrows(ValidationException.class, () ->
                authService.registerStudent("123456789-A", "Format", null, "Check", null, "pass1234", null));
        // Missing dash rejected
        assertThrows(ValidationException.class, () ->
                authService.registerStudent("12345678A", "Format", null, "Check", null, "pass1234", null));
        // Two letters rejected
        assertThrows(ValidationException.class, () ->
                authService.registerStudent("12345678-AB", "Format", null, "Check", null, "pass1234", null));
        // Missing letter rejected
        assertThrows(ValidationException.class, () ->
                authService.registerStudent("12345678-", "Format", null, "Check", null, "pass1234", null));

        // Lowercase letter accepted and stored normalized uppercase
        String lowercaseId = uniqueTestId().toLowerCase();
        User created = authService.registerStudent(lowercaseId, "Case", null, "Normalizer", null, "pass1234", null);
        assertEquals(lowercaseId.toUpperCase(), created.getStudentId());

        // Login is case-insensitive: lowercase input finds the uppercase account
        // (fresh registrations are PENDING, so a hit surfaces as PENDING_VERIFICATION,
        //  while a wrong ID would be USER_NOT_FOUND)
        assertEquals(AuthService.AuthStatus.PENDING_VERIFICATION,
                authService.authenticate(lowercaseId, "pass1234").getStatus());
        assertEquals(AuthService.AuthStatus.PENDING_VERIFICATION,
                authService.authenticate(created.getStudentId(), "pass1234").getStatus());
        assertEquals(AuthService.AuthStatus.USER_NOT_FOUND,
                authService.authenticate("99999999-Z", "pass1234").getStatus());
    }

    @Test
    public void testAdminLoginCredentialsTolerance() {
        AuthService authService = new AuthService();

        // Exact credentials
        assertEquals(AuthService.AuthStatus.SUCCESS,
                authService.authenticate("admin", "admin123").getStatus());
        // Capitalized username still resolves to the lowercase seeded admin
        assertEquals(AuthService.AuthStatus.SUCCESS,
                authService.authenticate("Admin", "admin123").getStatus());
        assertEquals(AuthService.AuthStatus.SUCCESS,
                authService.authenticate("ADMIN", "admin123").getStatus());
        // Clipboard whitespace must not lock the admin out
        assertEquals(AuthService.AuthStatus.SUCCESS,
                authService.authenticate("admin ", " admin123").getStatus());
        // Wrong credentials still fail cleanly
        assertEquals(AuthService.AuthStatus.INVALID_CREDENTIALS,
                authService.authenticate("admin", "wrongpass1").getStatus());
        assertEquals(AuthService.AuthStatus.USER_NOT_FOUND,
                authService.authenticate("noadmin", "admin123").getStatus());
    }

    @Test
    public void testAdminAddStudentValidation() {
        NetworkService netService = NetworkService.getInstance();

        // Blank section rejected by the service even though the dialog allowed it before
        assertThrows(ValidationException.class, () ->
                netService.adminAddStudent(uniqueTestId(), "Section", null, "Check", null, "BSIT", "1st Year", " "));

        // Student ID outside the 00000000-A format rejected
        assertThrows(ValidationException.class, () ->
                netService.adminAddStudent("bad*id", "Section", null, "Check", null, "BSIT", "1st Year", "BSIT 1A"));
    }

    @Test
    public void testChangePasswordWorkflow() throws Exception {
        AuthService authService = new AuthService();
        VerificationService verificationService = new VerificationService();

        String testId = uniqueTestId();
        authService.registerStudent(testId, "Password", null, "Changer", null, "pass1234", null);
        assertTrue(verificationService.approveStudent(testId));

        // Wrong current password is rejected
        assertEquals(AuthService.PasswordChangeResult.WRONG_CURRENT_PASSWORD,
                authService.changePassword(testId, "wrongpass1", "newpass99"));

        // New password without a digit is rejected
        assertThrows(ValidationException.class, () ->
                authService.changePassword(testId, "pass1234", "newpassword"));

        // Successful round-trip: old password dies, new password works
        assertEquals(AuthService.PasswordChangeResult.SUCCESS,
                authService.changePassword(testId, "pass1234", "newpass99"));
        assertEquals(AuthService.AuthStatus.INVALID_CREDENTIALS,
                authService.authenticate(testId, "pass1234").getStatus());
        assertEquals(AuthService.AuthStatus.SUCCESS,
                authService.authenticate(testId, "newpass99").getStatus());
    }

    @Test
    public void testAdminResetPasswordWorkflow() throws Exception {
        NetworkService netService = NetworkService.getInstance();
        AuthService authService = new AuthService();
        VerificationService verificationService = new VerificationService();

        String testId = uniqueTestId();
        authService.registerStudent(testId, "Resettable", null, "Student", null, "pass1234", null);
        assertTrue(verificationService.approveStudent(testId));

        // Weak new password rejected
        assertThrows(ValidationException.class, () ->
                netService.adminResetPassword(testId, "short"));

        // Administrator accounts can never be reset this way
        assertFalse(netService.adminResetPassword("admin", "newadminpass1"));

        // Successful reset: temporary password works, old one is dead
        assertTrue(netService.adminResetPassword(testId, "tempNew99"));
        assertEquals(AuthService.AuthStatus.SUCCESS,
                authService.authenticate(testId, "tempNew99").getStatus());
        assertEquals(AuthService.AuthStatus.INVALID_CREDENTIALS,
                authService.authenticate(testId, "pass1234").getStatus());
    }

    // ==================================================
    // ID-CARD PHOTO VALIDATION (content, not extension)
    // ==================================================

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private static File tempFile(String name, byte[]... chunks) throws IOException {
        Path path = Files.createTempFile("nfid-", name);
        for (byte[] chunk : chunks) {
            Files.write(path, chunk, java.nio.file.StandardOpenOption.APPEND);
        }
        File file = path.toFile();
        file.deleteOnExit();
        return file;
    }

    @Test
    public void testIdCardImageValidationRejectsSpoofedAndOversizedFiles() throws Exception {
        AuthService authService = new AuthService();

        // 1. Renamed non-image ("malware.exe" -> "photo.jpg") blocked by magic bytes
        File spoofed = tempFile(".jpg", "Totally not an image".getBytes());
        String spoofError = AuthService.validateIdCardImage(spoofed);
        assertEquals("Only PNG or JPG images are accepted.", spoofError);
        assertThrows(ValidationException.class, () ->
                authService.registerStudent(uniqueTestId(), "Spoof", null, "Check", null, "pass1234", spoofed));

        // 2. Real PNG magic bytes but oversized (>5 MB) blocked by size cap
        byte[] payload = new byte[5 * 1024 * 1024];
        File oversized = tempFile(".png", PNG_SIGNATURE, payload);
        assertEquals("The ID photo is too large. Please use an image under 5 MB.",
                AuthService.validateIdCardImage(oversized));

        // 3. A selected file that disappeared is rejected, not silently skipped
        File missing = new File(System.getProperty("java.io.tmpdir"), "vanished_" + System.nanoTime() + ".jpg");
        assertEquals("The selected ID photo no longer exists. Please choose it again.",
                AuthService.validateIdCardImage(missing));

        // 4. A genuine PNG-signature file passes the whole registration path
        File genuine = tempFile(".png", PNG_SIGNATURE, "fake-image-body".getBytes());
        String validId = uniqueTestId();
        User created = authService.registerStudent(validId, "Genuine", null, "Upload", null, "pass1234", genuine);
        assertNotNull(created);
        assertNotNull(created.getIdCardImagePath());
        assertTrue(created.getIdCardImagePath().endsWith(".png"));
    }
}
