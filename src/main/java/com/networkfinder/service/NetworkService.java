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

    public synchronized boolean addConnection(String student1, String student2) {
        if (!isValidConnectionPair(student1, student2)) {
            return false;
        }
        boolean dbSuccess = connectionDao.addConnection(student1, student2, ConnectionStatus.ACCEPTED);
        if (dbSuccess) {
            graph.addEdge(student1, student2);
        }
        return dbSuccess;
    }

    public synchronized boolean removeConnection(String student1, String student2) {
        boolean dbSuccess = connectionDao.removeConnection(student1, student2);
        if (dbSuccess) {
            graph.removeEdge(student1, student2);
        }
        return dbSuccess;
    }

    public boolean hasConnection(String student1, String student2) {
        return graph.hasEdge(student1, student2);
    }

    public synchronized boolean sendConnectionRequest(String requesterId, String receiverId) {
        if (!isValidConnectionPair(requesterId, receiverId)) {
            return false;
        }
        if (connectionDao.getConnectionBetween(requesterId, receiverId) != null) {
            return false;
        }
        return connectionDao.addConnection(requesterId, receiverId, ConnectionStatus.PENDING);
    }

    private boolean isValidConnectionPair(String id1, String id2) {
        if (id1 == null || id2 == null || id1.equals(id2)) {
            return false;
        }
        User user1 = userDao.getUserById(id1);
        User user2 = userDao.getUserById(id2);
        if (user1 == null || user2 == null) {
            return false;
        }
        return user1.getRole() == Role.STUDENT && user1.getVerificationStatus() == VerificationStatus.APPROVED
                && user2.getRole() == Role.STUDENT && user2.getVerificationStatus() == VerificationStatus.APPROVED;
    }

    public synchronized boolean acceptConnectionRequest(String requesterId, String receiverId) {
        boolean ok = connectionDao.updateConnectionStatus(requesterId, receiverId, ConnectionStatus.ACCEPTED);
        if (ok) {
            graph.addEdge(requesterId, receiverId);
        }
        return ok;
    }

    public synchronized boolean declineConnectionRequest(String requesterId, String receiverId) {
        return connectionDao.removeConnection(requesterId, receiverId);
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
    public synchronized boolean adminAddStudent(String studentId, String firstName, String middleInitial,
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
            return false;
        }
        if (!created) {
            return false;
        }

        StudentNode node = new StudentNode(cleanId, composedName, profile);
        graph.addVertex(node);
        return true;
    }

    public synchronized boolean adminEditStudent(String studentId, String firstName, String middleInitial,
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
        if (user == null) return false;

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
        return true;
    }

    public synchronized boolean adminDeleteStudent(String studentId) {
        if (studentId == null) {
            return false;
        }
        User user = userDao.getUserById(studentId);
        if (user == null || user.getRole() == Role.ADMIN) {
            return false;
        }
        boolean deleted = userDao.deleteUser(studentId);
        profileDao.deleteProfile(studentId);
        graph.removeVertex(studentId);
        return deleted;
    }

    /**
     * Admin-initiated password reset (the recovery path for forgotten
     * passwords). Admin accounts are protected; the new password is enforced
     * by the shared rules. Throws {@link ValidationException} for bad input,
     * returns false only for unknown targets/storage failure.
     */
    public synchronized boolean adminResetPassword(String studentId, String newPassword) throws ValidationException {
        String passwordError = InputValidator.checkPassword(newPassword);
        if (passwordError != null) {
            throw new ValidationException(passwordError);
        }
        if (studentId == null) {
            return false;
        }
        User user = userDao.getUserById(studentId.trim());
        if (user == null || user.getRole() == Role.ADMIN) {
            return false;
        }
        return userDao.updatePasswordHash(user.getStudentId(), DatabaseManager.hashPassword(newPassword));
    }

    public synchronized void adminResetDataset() {
        com.networkfinder.database.DataSeeder.forceResetAndSeed();
        reloadGraph();
    }

    public List<String[]> getAllAcceptedConnections() {
        return connectionDao.getAcceptedPairs();
    }
}
