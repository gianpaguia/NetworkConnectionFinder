package com.networkfinder.service;

import com.networkfinder.database.ConnectionDao;
import com.networkfinder.database.DatabaseManager;
import com.networkfinder.database.ProfileDao;
import com.networkfinder.database.UserDao;
import com.networkfinder.dsa.*;
import com.networkfinder.model.ConnectionStatus;
import com.networkfinder.model.Role;
import com.networkfinder.model.StudentProfile;
import com.networkfinder.model.User;
import com.networkfinder.model.VerificationStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class NetworkService {
    private static NetworkService instance;

    private final Graph graph = new Graph();
    private final UserDao userDao = new UserDao();
    private final ProfileDao profileDao = new ProfileDao();
    private final ConnectionDao connectionDao = new ConnectionDao();

    private NetworkService() {
        reloadGraph();
    }

    public static synchronized NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService();
        }
        return instance;
    }

    public synchronized void reloadGraph() {
        graph.clear();

        // 1. Load approved students with profiles as nodes
        List<User> approvedStudents = userDao.getApprovedStudents();
        for (User user : approvedStudents) {
            StudentProfile profile = profileDao.getProfile(user.getStudentId());
            if (profile == null) {
                profile = new StudentProfile();
                profile.setStudentId(user.getStudentId());
            }
            StudentNode node = new StudentNode(user.getStudentId(), user.getFullName(), profile);
            graph.addVertex(node);
        }

        // 2. Load accepted connection edges
        List<String[]> acceptedEdges = connectionDao.getAcceptedPairs();
        for (String[] edge : acceptedEdges) {
            graph.addEdge(edge[0], edge[1]);
        }
    }

    public Graph getGraph() {
        return graph;
    }

    public PathResult findShortestPath(String sourceStudentId, String targetStudentId) {
        return BFSPathfinder.findShortestPath(graph, sourceStudentId, targetStudentId);
    }

    public List<MatchResult> matchStudyGroups(String currentStudentId, MatchFilter filter) {
        return StudyGroupMatcher.findMatches(graph, currentStudentId, filter);
    }

    public List<MatchResult> matchCampusPeers(String currentStudentId, MatchFilter filter) {
        return StudyGroupMatcher.findCampusMatches(graph, currentStudentId, filter);
    }

    public List<String> getMutualConnections(String student1, String student2) {
        return NetworkMetrics.getMutualConnections(graph, student1, student2);
    }

    public double getCentrality(String studentId) {
        return NetworkMetrics.getDegreeCentrality(graph, studentId);
    }

    public List<StudentNode> getTopCoordinators(int limit) {
        return NetworkMetrics.getTopCoordinators(graph, limit);
    }

    public synchronized OperationResult addConnection(String student1, String student2) {
        String pairError = connectionPairError(student1, student2);
        if (pairError != null) {
            return OperationResult.fail(pairError);
        }
        com.networkfinder.model.Connection existing = connectionDao.getConnectionBetween(student1, student2);
        if (existing != null) {
            return OperationResult.fail(existing.getStatus() == ConnectionStatus.ACCEPTED
                    ? "You are already connected to " + nameOf(student2) + "."
                    : "A pending request with " + nameOf(student2) + " already exists.");
        }
        boolean dbSuccess = connectionDao.addConnection(student1, student2, ConnectionStatus.ACCEPTED);
        if (!dbSuccess) {
            return OperationResult.fail("Could not save this connection. Please try again.");
        }
        graph.addEdge(student1, student2);
        return OperationResult.ok();
    }

    public synchronized OperationResult removeConnection(String student1, String student2) {
        boolean dbSuccess = connectionDao.removeConnection(student1, student2);
        if (!dbSuccess) {
            return OperationResult.fail("No connection record was found between these students.");
        }
        graph.removeEdge(student1, student2);
        return OperationResult.ok("Connection removed.");
    }

    public boolean hasConnection(String student1, String student2) {
        return graph.hasEdge(student1, student2);
    }

    public synchronized OperationResult sendConnectionRequest(String requesterId, String receiverId) {
        String pairError = connectionPairError(requesterId, receiverId);
        if (pairError != null) {
            return OperationResult.fail(pairError);
        }
        com.networkfinder.model.Connection existing = connectionDao.getConnectionBetween(requesterId, receiverId);
        if (existing != null) {
            return OperationResult.fail(existing.getStatus() == ConnectionStatus.ACCEPTED
                    ? "You are already connected to " + nameOf(receiverId) + "."
                    : "A connection request is already pending with this student.");
        }
        boolean dbSuccess = connectionDao.addConnection(requesterId, receiverId, ConnectionStatus.PENDING);
        if (!dbSuccess) {
            return OperationResult.fail("Could not send the connection request. Please try again.");
        }
        return OperationResult.ok("Connection request sent to " + nameOf(receiverId) + ".");
    }

    private String connectionPairError(String id1, String id2) {
        if (id1 == null || id2 == null) {
            return "Select two students.";
        }
        if (id1.equals(id2)) {
            return "Select two different students.";
        }
        User user1 = userDao.getUserById(id1);
        User user2 = userDao.getUserById(id2);
        if (user1 == null) {
            return "No student account was found for " + id1 + ".";
        }
        if (user2 == null) {
            return "No student account was found for " + id2 + ".";
        }
        if (user1.getRole() == Role.ADMIN || user2.getRole() == Role.ADMIN) {
            return "Administrator accounts cannot be connected.";
        }
        if (user1.getVerificationStatus() != VerificationStatus.APPROVED
                || user2.getVerificationStatus() != VerificationStatus.APPROVED) {
            return "Both students must be verified before connecting.";
        }
        return null;
    }

    private String nameOf(String studentId) {
        User user = userDao.getUserById(studentId);
        return user != null ? user.getFullName() : studentId;
    }

    public synchronized OperationResult acceptConnectionRequest(String requesterId, String receiverId) {
        boolean ok = connectionDao.updateConnectionStatus(requesterId, receiverId, ConnectionStatus.ACCEPTED);
        if (!ok) {
            return OperationResult.fail("No pending request was found between these students.");
        }
        graph.addEdge(requesterId, receiverId);
        return OperationResult.ok("Request accepted - you are now connected.");
    }

    public synchronized OperationResult declineConnectionRequest(String requesterId, String receiverId) {
        boolean ok = connectionDao.removeConnection(requesterId, receiverId);
        if (!ok) {
            return OperationResult.fail("No pending request was found between these students.");
        }
        return OperationResult.ok("Request declined.");
    }

    public com.networkfinder.model.Connection getConnectionBetween(String u, String v) {
        return connectionDao.getConnectionBetween(u, v);
    }

    public List<com.networkfinder.model.Connection> getIncomingRequests(String studentId) {
        return connectionDao.getPendingIncomingRequests(studentId);
    }

    public List<com.networkfinder.model.Connection> getOutgoingRequests(String studentId) {
        return connectionDao.getPendingOutgoingRequests(studentId);
    }

    // ==================================================
    // ADMIN DATA MANAGEMENT & INTEGRITY METHODS
    // ==================================================

    /**
     * Admin-only manual student creation. Input is validated by the service
     * regardless of what the dialog allowed; validation failures throw
     * {@link ValidationException} with a presentable message. Returns false
     * only for storage-level failure.
     */
    public synchronized OperationResult adminAddStudent(String studentId, String firstName, String middleInitial,
                                                        String surname, String suffix, String course,
                                                        String yearLevel, String section) throws ValidationException {
        String idError = InputValidator.checkStudentId(studentId);
        if (idError != null) {
            throw new ValidationException(idError);
        }
        String firstNameError = InputValidator.checkFirstName(firstName);
        if (firstNameError != null) {
            throw new ValidationException(firstNameError);
        }
        String surnameError = InputValidator.checkSurname(surname);
        if (surnameError != null) {
            throw new ValidationException(surnameError);
        }
        String middleError = InputValidator.checkMiddleInitial(middleInitial);
        if (middleError != null) {
            throw new ValidationException(middleError);
        }
        String suffixError = InputValidator.checkSuffix(suffix);
        if (suffixError != null) {
            throw new ValidationException(suffixError);
        }
        String courseError = InputValidator.checkCourse(course);
        if (courseError != null) {
            throw new ValidationException(courseError);
        }
        String sectionError = InputValidator.checkSection(section);
        if (sectionError != null) {
            throw new ValidationException(sectionError);
        }
        if (userDao.getUserById(studentId) != null) {
            throw new ValidationException("This Student ID is already registered.");
        }

        String cleanId = studentId.trim();
        String cleanFirst = firstName.trim();
        String cleanMiddle = middleInitial == null ? "" : middleInitial.trim();
        String cleanSurname = surname.trim();
        String cleanSuffix = suffix == null ? "" : suffix.trim();
        String cleanCourse = course == null ? "" : course.trim();
        String cleanSection = section == null ? "" : section.trim();

        String passwordHash = DatabaseManager.hashPassword(DatabaseManager.DEFAULT_DEMO_PASSWORD);
        User user = new User();
        user.setStudentId(cleanId);
        user.setNames(cleanFirst, cleanMiddle, cleanSurname, cleanSuffix);
        user.setPasswordHash(passwordHash);
        user.setRole(Role.STUDENT);
        user.setVerificationStatus(VerificationStatus.APPROVED);
        user.setProfileCompleted(true);
        user.setCreatedAt(java.time.LocalDateTime.now());
        String composedName = user.getFullName();
        StudentProfile profile = new StudentProfile(
                cleanId, cleanCourse, yearLevel, cleanSection,
                new java.util.ArrayList<>(), new java.util.ArrayList<>(), "Small Group",
                new java.util.ArrayList<>(), ""
        );

        boolean created;
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                created = userDao.createUser(user, conn) && profileDao.saveOrUpdateProfile(profile, conn);
                if (created) {
                    conn.commit();
                } else {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                try {
                    conn.rollback();
                } catch (SQLException ignoredRollback) {
                }
                created = false;
            } finally {
                conn.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return OperationResult.fail("Could not save the student. Please try again.");
        }
        if (!created) {
            return OperationResult.fail("Could not save the student. Please try again.");
        }

        StudentNode node = new StudentNode(cleanId, composedName, profile);
        graph.addVertex(node);
        return OperationResult.ok("Student created with default password password123.");
    }

    public synchronized OperationResult adminEditStudent(String studentId, String firstName, String middleInitial,
                                                         String surname, String suffix, String course,
                                                         String yearLevel, String section) throws ValidationException {
        String firstNameError = InputValidator.checkFirstName(firstName);
        if (firstNameError != null) {
            throw new ValidationException(firstNameError);
        }
        String surnameError = InputValidator.checkSurname(surname);
        if (surnameError != null) {
            throw new ValidationException(surnameError);
        }
        String middleError = InputValidator.checkMiddleInitial(middleInitial);
        if (middleError != null) {
            throw new ValidationException(middleError);
        }
        String suffixError = InputValidator.checkSuffix(suffix);
        if (suffixError != null) {
            throw new ValidationException(suffixError);
        }
        String courseError = InputValidator.checkCourse(course);
        if (courseError != null) {
            throw new ValidationException(courseError);
        }
        String sectionError = InputValidator.checkSection(section);
        if (sectionError != null) {
            throw new ValidationException(sectionError);
        }

        User user = userDao.getUserById(studentId);
        if (user == null) {
            return OperationResult.fail("No student account was found for " + studentId + ".");
        }

        String cleanFirst = firstName.trim();
        String cleanMiddle = middleInitial == null ? "" : middleInitial.trim();
        String cleanSurname = surname.trim();
        String cleanSuffix = suffix == null ? "" : suffix.trim();

        userDao.updateStudentNames(studentId, cleanFirst, cleanMiddle, cleanSurname, cleanSuffix);
        String composedName = com.networkfinder.model.User.composeFullName(cleanFirst, cleanMiddle, cleanSurname, cleanSuffix);

        StudentProfile profile = profileDao.getProfile(studentId);
        if (profile == null) {
            profile = new StudentProfile();
            profile.setStudentId(studentId);
        }
        profile.setCourse(course == null ? "" : course.trim());
        profile.setYearLevel(yearLevel);
        profile.setSection(section == null ? "" : section.trim());
        profileDao.saveOrUpdateProfile(profile);

        StudentNode existingNode = graph.getNode(studentId);
        if (existingNode != null) {
            existingNode.setFullName(composedName);
            existingNode.setProfile(profile);
        }
        return OperationResult.ok("Student profile updated.");
    }

    public synchronized OperationResult adminDeleteStudent(String studentId) {
        if (studentId == null) {
            return OperationResult.fail("Select a student to delete.");
        }
        User user = userDao.getUserById(studentId);
        if (user == null) {
            return OperationResult.fail("No student account was found for " + studentId + ".");
        }
        if (user.getRole() == Role.ADMIN) {
            return OperationResult.fail("Administrator accounts cannot be deleted.");
        }
        boolean deleted = userDao.deleteUser(studentId);
        if (!deleted) {
            return OperationResult.fail("Could not delete the student. Please try again.");
        }
        profileDao.deleteProfile(studentId);
        graph.removeVertex(studentId);
        return OperationResult.ok("Student deleted.");
    }

    /**
     * Admin-initiated password reset (the recovery path for forgotten
     * passwords). Admin accounts are protected; the new password is enforced
     * by the shared rules. Throws {@link ValidationException} for bad input,
     * returns false only for unknown targets/storage failure.
     */
    public synchronized OperationResult adminResetPassword(String studentId, String newPassword) throws ValidationException {
        String passwordError = InputValidator.checkPassword(newPassword);
        if (passwordError != null) {
            throw new ValidationException(passwordError);
        }
        if (studentId == null) {
            return OperationResult.fail("Select an account to reset.");
        }
        User user = userDao.getUserById(studentId.trim());
        if (user == null) {
            return OperationResult.fail("No student account was found for this ID.");
        }
        if (user.getRole() == Role.ADMIN) {
            return OperationResult.fail("Administrator accounts cannot be reset here.");
        }
        boolean updated = userDao.updatePasswordHash(user.getStudentId(), DatabaseManager.hashPassword(newPassword));
        if (!updated) {
            return OperationResult.fail("Could not update the password. Please try again.");
        }
        return OperationResult.ok("Password reset - the new password can be used to log in.");
    }

    // ==================================================
    // READ FACADE - views must read through the service, not build DAOs
    // ==================================================

    public StudentProfile getStudentProfile(String studentId) {
        return profileDao.getProfile(studentId);
    }

    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }

    public List<StudentProfile> getAllProfiles() {
        return profileDao.getAllProfiles();
    }

    public void saveStudentProfile(String studentId, StudentProfile profile) {
        profileDao.saveOrUpdateProfile(profile);
        userDao.updateProfileCompleted(studentId, true);
        StudentNode node = graph.getNode(studentId);
        if (node != null) {
            node.setProfile(profile);
        }
    }

    public synchronized void adminResetDataset() {
        com.networkfinder.database.DataSeeder.forceResetAndSeed();
        reloadGraph();
    }

    public List<String[]> getAllAcceptedConnections() {
        return connectionDao.getAcceptedPairs();
    }
}
