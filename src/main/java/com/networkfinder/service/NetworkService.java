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
        return connectionDao.addConnection(requesterId, receiverId, ConnectionStatus.PENDING);
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

    public synchronized boolean adminAddStudent(String studentId, String fullName, String course, String yearLevel, String section) {
        if (studentId == null || fullName == null || userDao.getUserById(studentId) != null) {
            return false;
        }
        String passwordHash = com.networkfinder.database.DatabaseManager.hashPassword("password123");
        User user = new User(
                studentId, fullName, passwordHash, null,
                com.networkfinder.model.Role.STUDENT,
                com.networkfinder.model.VerificationStatus.APPROVED,
                true,
                java.time.LocalDateTime.now()
        );
        boolean created = userDao.createUser(user);
        if (!created) return false;

        StudentProfile profile = new StudentProfile(
                studentId, course, yearLevel, section,
                new java.util.ArrayList<>(), new java.util.ArrayList<>(), "Small Group",
                new java.util.ArrayList<>(), ""
        );
        profileDao.saveOrUpdateProfile(profile);

        StudentNode node = new StudentNode(studentId, fullName, profile);
        graph.addVertex(node);
        return true;
    }

    public synchronized boolean adminEditStudent(String studentId, String fullName, String course, String yearLevel, String section) {
        User user = userDao.getUserById(studentId);
        if (user == null) return false;

        userDao.updateStudentInfo(studentId, fullName);

        StudentProfile profile = profileDao.getProfile(studentId);
        if (profile == null) {
            profile = new StudentProfile();
            profile.setStudentId(studentId);
        }
        profile.setCourse(course);
        profile.setYearLevel(yearLevel);
        profile.setSection(section);
        profileDao.saveOrUpdateProfile(profile);

        StudentNode existingNode = graph.getNode(studentId);
        if (existingNode != null) {
            existingNode.setFullName(fullName);
            existingNode.setProfile(profile);
        }
        return true;
    }

    public synchronized boolean adminDeleteStudent(String studentId) {
        if (studentId == null) return false;
        boolean deleted = userDao.deleteUser(studentId);
        profileDao.deleteProfile(studentId);
        graph.removeVertex(studentId);
        return deleted;
    }

    public synchronized void adminResetDataset() {
        com.networkfinder.database.DataSeeder.forceResetAndSeed();
        reloadGraph();
    }

    public List<String[]> getAllAcceptedConnections() {
        return connectionDao.getAcceptedPairs();
    }
}
