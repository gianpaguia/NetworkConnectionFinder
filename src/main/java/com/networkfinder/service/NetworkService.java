package com.networkfinder.service;

import com.networkfinder.database.ConnectionDao;
import com.networkfinder.database.ProfileDao;
import com.networkfinder.database.UserDao;
import com.networkfinder.dsa.*;
import com.networkfinder.model.ConnectionStatus;
import com.networkfinder.model.StudentProfile;
import com.networkfinder.model.User;

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

        // 1. Load all profiles once (single query) and index them by student_id
        java.util.Map<String, StudentProfile> profilesById = new java.util.HashMap<>();
        for (StudentProfile p : profileDao.getAllProfiles()) {
            profilesById.put(p.getStudentId(), p);
        }

        // 2. Load approved students as graph vertices
        List<User> approvedStudents = userDao.getApprovedStudents();
        for (User user : approvedStudents) {
            StudentProfile profile = profilesById.get(user.getStudentId());
            if (profile == null) {
                profile = new StudentProfile();
                profile.setStudentId(user.getStudentId());
            }
            StudentNode node = new StudentNode(user.getStudentId(), user.getFullName(), profile);
            graph.addVertex(node);
        }

        // 3. Load accepted connection edges
        List<String[]> acceptedEdges = connectionDao.getAcceptedPairs();
        for (String[] edge : acceptedEdges) {
            graph.addEdge(edge[0], edge[1]);
        }
    }

    public Graph getGraph() {
        return graph;
    }

    // ==================================================
    // READ FACADE — the UI layer goes through here, never through DAOs directly
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

    // Persists a completed profile, flags the account as profile-complete and syncs the graph node
    public OperationResult saveStudentProfile(String studentId, StudentProfile profile) {
        if (studentId == null || profile == null || !studentId.equals(profile.getStudentId())) {
            return OperationResult.fail("Profile does not belong to the selected student.");
        }
        if (!profileDao.saveOrUpdateProfile(profile)) {
            return OperationResult.fail("Database rejected the profile update. Please try again.");
        }
        userDao.updateProfileCompleted(studentId, true);

        StudentNode node = graph.getNode(studentId);
        if (node != null) {
            node.setProfile(profile);
        }
        return OperationResult.ok();
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
        if (student1 == null || student2 == null) {
            return OperationResult.fail("Both students must be selected.");
        }
        if (student1.equals(student2)) {
            return OperationResult.fail("A student cannot be connected to themselves.");
        }
        if (!graph.containsVertex(student1) || !graph.containsVertex(student2)) {
            return OperationResult.fail("One of the students is no longer part of the network.");
        }
        if (graph.hasEdge(student1, student2) || connectionDao.hasConnection(student1, student2)) {
            return OperationResult.fail("An edge between " + student1 + " and " + student2 + " already exists.");
        }
        if (connectionDao.addConnection(student1, student2, ConnectionStatus.ACCEPTED)) {
            graph.addEdge(student1, student2);
            return OperationResult.ok();
        }
        return OperationResult.fail("Database rejected the connection. Please try again.");
    }

    public synchronized OperationResult removeConnection(String student1, String student2) {
        if (connectionDao.removeConnection(student1, student2)) {
            graph.removeEdge(student1, student2);
            return OperationResult.ok("Connection removed.");
        }
        return OperationResult.fail("No link exists between " + student1 + " and " + student2 + ".");
    }

    public boolean hasConnection(String student1, String student2) {
        return graph.hasEdge(student1, student2);
    }

    public synchronized OperationResult sendConnectionRequest(String requesterId, String receiverId) {
        if (requesterId == null || receiverId == null) {
            return OperationResult.fail("Both students must be selected.");
        }
        if (requesterId.equals(receiverId)) {
            return OperationResult.fail("You cannot send a connection request to yourself.");
        }
        if (!graph.containsVertex(requesterId) || !graph.containsVertex(receiverId)) {
            return OperationResult.fail("One of the students is no longer part of the network.");
        }
        if (graph.hasEdge(requesterId, receiverId)) {
            return OperationResult.fail("You are already connected to " + receiverId + ".");
        }
        if (connectionDao.hasConnection(requesterId, receiverId)) {
            return OperationResult.fail("An active link or pending request with " + receiverId + " already exists.");
        }
        if (connectionDao.addConnection(requesterId, receiverId, ConnectionStatus.PENDING)) {
            return OperationResult.ok("Connection request sent to " + receiverId + ".");
        }
        return OperationResult.fail("Database rejected the request. Please try again.");
    }

    public synchronized OperationResult acceptConnectionRequest(String requesterId, String receiverId) {
        if (connectionDao.updateConnectionStatus(requesterId, receiverId, ConnectionStatus.ACCEPTED)) {
            graph.addEdge(requesterId, receiverId);
            return OperationResult.ok("Connection established.");
        }
        return OperationResult.fail("No pending request was found between these students.");
    }

    public synchronized OperationResult declineConnectionRequest(String requesterId, String receiverId) {
        if (connectionDao.removeConnection(requesterId, receiverId)) {
            return OperationResult.ok("Request removed.");
        }
        return OperationResult.fail("No pending request was found between these students.");
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

    private static final int MAX_PROFILE_FIELD_LENGTH = 100;

    // Trimmed, non-blank, length-capped input; null when invalid
    private static String normalizeInput(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        if (cleaned.isEmpty() || cleaned.length() > maxLength) {
            return null;
        }
        return cleaned;
    }

    public synchronized OperationResult adminAddStudent(String studentId, String fullName, String course, String yearLevel, String section) {
        String cleanId = AuthService.validateStudentId(studentId);
        String cleanName = normalizeInput(fullName, MAX_PROFILE_FIELD_LENGTH);
        String cleanCourse = normalizeInput(course, MAX_PROFILE_FIELD_LENGTH);
        String cleanYear = normalizeInput(yearLevel, MAX_PROFILE_FIELD_LENGTH);
        String cleanSection = normalizeInput(section, MAX_PROFILE_FIELD_LENGTH);
        if (cleanId == null) {
            return OperationResult.fail("Student ID must start with a letter or number and may only contain letters, numbers, dots, dashes or underscores (max 64 characters).");
        }
        if (cleanName == null) {
            return OperationResult.fail("Full Name is required (max 100 characters).");
        }
        if (cleanCourse == null || cleanYear == null || cleanSection == null) {
            return OperationResult.fail("Course, Year Level and Section are all required (max 100 characters each).");
        }
        if (userDao.getUserById(cleanId) != null) {
            return OperationResult.fail("Student ID " + cleanId + " is already registered.");
        }
        String passwordHash = com.networkfinder.database.DatabaseManager.hashPassword("password123");
        User user = new User(
                cleanId, cleanName, passwordHash, null,
                com.networkfinder.model.Role.STUDENT,
                com.networkfinder.model.VerificationStatus.APPROVED,
                true,
                java.time.LocalDateTime.now()
        );
        if (!userDao.createUser(user)) {
            return OperationResult.fail("Database rejected the new student. Please try again.");
        }

        StudentProfile profile = new StudentProfile(
                cleanId, cleanCourse, cleanYear, cleanSection,
                new java.util.ArrayList<>(), new java.util.ArrayList<>(), "Small Group",
                new java.util.ArrayList<>(), ""
        );
        profileDao.saveOrUpdateProfile(profile);

        StudentNode node = new StudentNode(cleanId, cleanName, profile);
        graph.addVertex(node);
        return OperationResult.ok("Student created with default password password123.");
    }

    public synchronized OperationResult adminEditStudent(String studentId, String fullName, String course, String yearLevel, String section) {
        User user = userDao.getUserById(studentId);
        if (user == null) {
            return OperationResult.fail("Unknown student: " + studentId + ".");
        }
        String cleanName = normalizeInput(fullName, MAX_PROFILE_FIELD_LENGTH);
        String cleanCourse = normalizeInput(course, MAX_PROFILE_FIELD_LENGTH);
        String cleanYear = normalizeInput(yearLevel, MAX_PROFILE_FIELD_LENGTH);
        String cleanSection = normalizeInput(section, MAX_PROFILE_FIELD_LENGTH);
        if (cleanName == null || cleanCourse == null || cleanYear == null || cleanSection == null) {
            return OperationResult.fail("Full Name, Course, Year Level and Section are all required (max 100 characters each).");
        }

        userDao.updateStudentInfo(studentId, cleanName);

        StudentProfile profile = profileDao.getProfile(studentId);
        if (profile == null) {
            profile = new StudentProfile();
            profile.setStudentId(studentId);
        }
        profile.setCourse(cleanCourse);
        profile.setYearLevel(cleanYear);
        profile.setSection(cleanSection);
        profileDao.saveOrUpdateProfile(profile);

        StudentNode existingNode = graph.getNode(studentId);
        if (existingNode != null) {
            existingNode.setFullName(cleanName);
            existingNode.setProfile(profile);
        }
        return OperationResult.ok("Student updated.");
    }

    public synchronized OperationResult adminDeleteStudent(String studentId) {
        if (studentId == null) {
            return OperationResult.fail("No student was selected.");
        }
        if (!userDao.deleteUser(studentId)) {
            return OperationResult.fail("Unknown student: " + studentId + ".");
        }
        connectionDao.deleteConnectionsForStudent(studentId);
        profileDao.deleteProfile(studentId);
        graph.removeVertex(studentId);
        return OperationResult.ok("Student removed.");
    }

    public synchronized void adminResetDataset() {
        com.networkfinder.database.DataSeeder.forceResetAndSeed();
        reloadGraph();
    }

    public List<String[]> getAllAcceptedConnections() {
        return connectionDao.getAcceptedPairs();
    }
}
