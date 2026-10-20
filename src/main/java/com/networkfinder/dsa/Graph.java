package com.networkfinder.dsa;

import java.util.*;

/**
 * Undirected Graph modeling the student network via an Adjacency List.
 * Vertices are StudentNode instances identified by their studentId.
 * Edges represent mutual academic connections between students.
 */
public class Graph {
    private final Map<String, StudentNode> nodes = new HashMap<>();
    private final Map<String, Set<String>> adjacencyList = new HashMap<>();

    public Graph() {
    }

    /**
     * Adds a student vertex to the graph.
     */
    public synchronized void addVertex(StudentNode node) {
        if (node == null || node.getStudentId() == null) {
            throw new IllegalArgumentException("Node and studentId cannot be null");
        }
        String id = node.getStudentId();
        nodes.put(id, node);
        adjacencyList.putIfAbsent(id, new LinkedHashSet<>());
    }

    /**
     * Removes a student vertex and all incident edges from the graph.
     */
    public synchronized boolean removeVertex(String studentId) {
        if (studentId == null || !nodes.containsKey(studentId)) {
            return false;
        }
        nodes.remove(studentId);
        Set<String> neighbors = adjacencyList.remove(studentId);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                Set<String> neighborAdj = adjacencyList.get(neighbor);
                if (neighborAdj != null) {
                    neighborAdj.remove(studentId);
                }
            }
        }
        return true;
    }

    /**
     * Adds an undirected edge between two students.
     */
    public synchronized boolean addEdge(String studentId1, String studentId2) {
        if (studentId1 == null || studentId2 == null || studentId1.equals(studentId2)) {
            return false;
        }
        if (!nodes.containsKey(studentId1) || !nodes.containsKey(studentId2)) {
            return false;
        }

        adjacencyList.get(studentId1).add(studentId2);
        adjacencyList.get(studentId2).add(studentId1);
        return true;
    }

    /**
     * Removes an undirected edge between two students.
     */
    public synchronized boolean removeEdge(String studentId1, String studentId2) {
        if (studentId1 == null || studentId2 == null) {
            return false;
        }
        boolean removed1 = false;
        boolean removed2 = false;

        Set<String> adj1 = adjacencyList.get(studentId1);
        if (adj1 != null) {
            removed1 = adj1.remove(studentId2);
        }
        Set<String> adj2 = adjacencyList.get(studentId2);
        if (adj2 != null) {
            removed2 = adj2.remove(studentId1);
        }

        return removed1 || removed2;
    }

    /**
     * Checks if an edge exists between two students.
     */
    public synchronized boolean hasEdge(String studentId1, String studentId2) {
        Set<String> adj = adjacencyList.get(studentId1);
        return adj != null && adj.contains(studentId2);
    }

    /**
     * Gets a defensive snapshot of the neighbor student IDs for a given student.
     */
    public synchronized Set<String> getNeighbors(String studentId) {
        Set<String> adj = adjacencyList.get(studentId);
        if (adj == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(adj));
    }

    /**
     * Gets the degree (number of connections) of a student.
     */
    public synchronized int getDegree(String studentId) {
        Set<String> adj = adjacencyList.get(studentId);
        return adj == null ? 0 : adj.size();
    }

    /**
     * Retrieves the StudentNode by studentId.
     */
    public synchronized StudentNode getNode(String studentId) {
        return nodes.get(studentId);
    }

    /**
     * Checks if a vertex exists in the graph.
     */
    public synchronized boolean containsVertex(String studentId) {
        return nodes.containsKey(studentId);
    }

    /**
     * Gets a defensive snapshot of all student nodes in the graph.
     */
    public synchronized Collection<StudentNode> getAllNodes() {
        return Collections.unmodifiableCollection(new ArrayList<>(nodes.values()));
    }

    /**
     * Gets a defensive snapshot of all vertex IDs.
     */
    public synchronized Set<String> getAllVertexIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(nodes.keySet()));
    }

    /**
     * Returns total number of vertices (students).
     */
    public synchronized int getVertexCount() {
        return nodes.size();
    }

    /**
     * Returns total number of undirected edges.
     */
    public synchronized int getEdgeCount() {
        int total = 0;
        for (Set<String> adj : adjacencyList.values()) {
            total += adj.size();
        }
        return total / 2;
    }

    /**
     * Clears all nodes and edges from the graph.
     */
    public synchronized void clear() {
        nodes.clear();
        adjacencyList.clear();
    }
}
