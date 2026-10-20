package com.networkfinder.dsa;

import java.util.*;

/**
 * Computes graph network metrics:
 * - Mutual connections (intersection of adjacency lists)
 * - Degree centrality (hub / study coordinator identification)
 */
public class NetworkMetrics {

    /**
     * Finds mutual direct connections between two students: Adj(u) ∩ Adj(v).
     */
    public static List<String> getMutualConnections(Graph graph, String student1, String student2) {
        if (graph == null || student1 == null || student2 == null) {
            return Collections.emptyList();
        }

        Set<String> adj1 = graph.getNeighbors(student1);
        Set<String> adj2 = graph.getNeighbors(student2);

        Set<String> intersection = new HashSet<>(adj1);
        intersection.retainAll(adj2);

        return new ArrayList<>(intersection);
    }

    /**
     * Calculates Degree Centrality: C_D(v) = deg(v) / (N - 1)
     * where N is total vertices.
     */
    public static double getDegreeCentrality(Graph graph, String studentId) {
        if (graph == null || studentId == null || !graph.containsVertex(studentId)) {
            return 0.0;
        }

        int n = graph.getVertexCount();
        if (n <= 1) {
            return 0.0;
        }

        int degree = graph.getDegree(studentId);
        return (double) degree / (n - 1);
    }

    /**
     * Finds top connected students in the network (e.g. potential study leaders).
     */
    public static List<StudentNode> getTopCoordinators(Graph graph, int limit) {
        if (graph == null || limit <= 0) {
            return Collections.emptyList();
        }

        List<StudentNode> allNodes = new ArrayList<>(graph.getAllNodes());
        allNodes.sort((a, b) -> Integer.compare(
                graph.getDegree(b.getStudentId()), 
                graph.getDegree(a.getStudentId())
        ));

        return allNodes.subList(0, Math.min(limit, allNodes.size()));
    }
}
