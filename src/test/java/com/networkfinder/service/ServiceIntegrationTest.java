package com.networkfinder.service;

import com.networkfinder.database.DataSeeder;
import com.networkfinder.database.DatabaseManager;
import com.networkfinder.database.UserDao;
import com.networkfinder.dsa.MatchFilter;
import com.networkfinder.dsa.MatchResult;
import com.networkfinder.dsa.PathResult;
import com.networkfinder.model.Role;
import com.networkfinder.model.User;
import com.networkfinder.model.VerificationStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceIntegrationTest {

    private static final java.util.concurrent.atomic.AtomicLong TEST_SEQ = new java.util.concurrent.atomic.AtomicLong();

    /** Unique, rule-compliant student ID for each test run (leaves the benchmark dataset untouched). */
    private static String uniqueTestId() {
        return "2026-TEST-" + System.currentTimeMillis() + "-" + TEST_SEQ.incrementAndGet();
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

        String testId = "2026-TEST-" + System.currentTimeMillis();
        // 1. Register student
        User student = authService.registerStudent(testId, "Test Student", "pass1234", null);
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

        // Check seeded student Juan Dela Cruz (2026-10001) to Daniel Padilla (2026-10009)
        // Expected route: Juan -> Maria -> Patricia -> Christian -> Daniel (or via Angel)
        PathResult pathResult = netService.findShortestPath("2026-10001", "2026-10009");
        assertTrue(pathResult.isReachable());
        assertTrue(pathResult.getDegreesOfSeparation() >= 3);
        assertTrue(pathResult.getPath().contains("2026-10001"));
        assertTrue(pathResult.getPath().contains("2026-10009"));
    }

    @Test
    public void testStudyGroupMatcherLevelOrderOnSeededData() {
        NetworkService netService = NetworkService.getInstance();
        netService.reloadGraph();

        // Search for Programming peers from Juan Dela Cruz (2026-10001)
        MatchFilter filter = new MatchFilter().setRequiredSubject("Programming");
        List<MatchResult> matches = netService.matchStudyGroups("2026-10001", filter);

        assertFalse(matches.isEmpty());
        // Closest match should be 1st degree peer Maria Santos
        assertEquals("2026-10002", matches.get(0).getStudent().getStudentId());
        assertEquals(1, matches.get(0).getDegreeOfSeparation());
    }

    @Test
    public void testMutualConnectionsOnSeededData() {
        NetworkService netService = NetworkService.getInstance();
        netService.reloadGraph();

        // Both Bea (2026-10006) and Joshua (2026-10007) connect to Mark (2026-10003)
        List<String> mutuals = netService.getMutualConnections("2026-10006", "2026-10007");
        assertTrue(mutuals.contains("2026-10003"));
    }

    @Test
    public void testTwoWayConnectionRequestWorkflow() {
        NetworkService netService = NetworkService.getInstance();
        netService.reloadGraph();

        String s1 = "2026-10001"; // Juan
        String s7 = "2026-10007"; // Joshua (not directly connected to Juan initially)

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
        String testStudentId = "2026-ADMIN-ADD";
        assertTrue(netService.adminAddStudent(testStudentId, "Admin Added Student", "BSIT", "1st Year", "BSIT 1A"));
        assertTrue(netService.getGraph().containsVertex(testStudentId));
        assertEquals("Admin Added Student", netService.getGraph().getNode(testStudentId).getFullName());

        // 2. Admin edits student
        assertTrue(netService.adminEditStudent(testStudentId, "Renamed Student", "BSCS", "2nd Year", "BSCS 2A"));
        assertEquals("Renamed Student", netService.getGraph().getNode(testStudentId).getFullName());
        assertEquals("BSCS 2A", netService.getGraph().getNode(testStudentId).getProfile().getSection());

        // 3. Admin adds connection between test student and Juan
        assertTrue(netService.addConnection(testStudentId, "2026-10001"));
        assertTrue(netService.hasConnection(testStudentId, "2026-10001"));

        // 4. Admin deletes student
        assertTrue(netService.adminDeleteStudent(testStudentId));
        assertFalse(netService.getGraph().containsVertex(testStudentId));
        assertFalse(netService.hasConnection(testStudentId, "2026-10001"));

        // 5. Admin resets dataset to initial benchmark state
        netService.adminResetDataset();
        assertEquals(10, netService.getGraph().getVertexCount());
        assertEquals(11, netService.getGraph().getEdgeCount());
    }

    // ==================================================
    // VALIDATION AUTHORITY (service rejects bad input)
    // ==================================================

    @Test
    public void testRegisterValidationRejectsBadInput() throws Exception {
        AuthService authService = new AuthService();

        // Student ID with a space (regex violation)
        assertThrows(ValidationException.class, () ->
                authService.registerStudent("bad id!", "Valid Name", "pass1234", null));

        // Password below the minimum length
        assertThrows(ValidationException.class, () ->
                authService.registerStudent(uniqueTestId(), "Valid Name", "short1", null));

        // Password without a digit
        assertThrows(ValidationException.class, () ->
                authService.registerStudent(uniqueTestId(), "Valid Name", "nodigitpass", null));

        // Blank full name
        assertThrows(ValidationException.class, () ->
                authService.registerStudent(uniqueTestId(), "   ", "pass1234", null));

        // Name containing digits
        assertThrows(ValidationException.class, () ->
                authService.registerStudent(uniqueTestId(), "Juan 123", "pass1234", null));

        // Duplicate Student ID
        String duplicateId = uniqueTestId();
        authService.registerStudent(duplicateId, "First User", "pass1234", null);
        assertThrows(ValidationException.class, () ->
                authService.registerStudent(duplicateId, "Second User", "pass1234", null));
    }

    @Test
    public void testAdminAddStudentValidation() {
        NetworkService netService = NetworkService.getInstance();

        // Blank section rejected by the service even though the dialog allowed it before
        assertThrows(ValidationException.class, () ->
                netService.adminAddStudent(uniqueTestId(), "Section Check", "BSIT", "1st Year", " "));

        // Password-like garbage in the ID (invalid characters)
        assertThrows(ValidationException.class, () ->
                netService.adminAddStudent("bad*id", "Section Check", "BSIT", "1st Year", "BSIT 1A"));
    }

    @Test
    public void testChangePasswordWorkflow() throws Exception {
        AuthService authService = new AuthService();
        VerificationService verificationService = new VerificationService();

        String testId = uniqueTestId();
        authService.registerStudent(testId, "Password Changer", "pass1234", null);
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
                authService.authenticate(testId, "pass123").getStatus());
        assertEquals(AuthService.AuthStatus.SUCCESS,
                authService.authenticate(testId, "newpass99").getStatus());
    }

    @Test
    public void testAdminResetPasswordWorkflow() throws Exception {
        NetworkService netService = NetworkService.getInstance();
        AuthService authService = new AuthService();
        VerificationService verificationService = new VerificationService();

        String testId = uniqueTestId();
        authService.registerStudent(testId, "Resettable Student", "pass1234", null);
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
                authService.authenticate(testId, "pass123").getStatus());
    }
}
