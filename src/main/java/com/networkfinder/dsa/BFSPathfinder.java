package com.networkfinder.dsa;

import java.util.*;

/**
 * Standard Breadth-First Search (BFS) to find the shortest connection path
 * (degrees of separation) between two specific students in the graph.
 */
public class BFSPathfinder {

    /**
     * Executes BFS to locate the shortest path between sourceId and targetId.
     *
     * @param graph The student network graph
     * @param sourceId The starting student's ID
     * @param targetId The destination student's ID
     * @return PathResult containing path, degrees of separation, and exploration metrics
     */
    public static PathResult findShortestPath(Graph graph, String sourceId, String targetId) {
        if (graph == null || sourceId == null || targetId == null) {
            return PathResult.unreachable(0);
        }
        if (!graph.containsVertex(sourceId) || !graph.containsVertex(targetId)) {
            return PathResult.unreachable(0);
        }

        // Trivial case: source is target
        if (sourceId.equals(targetId)) {
            return new PathResult(true, List.of(sourceId), 0, 1);
        }

        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        Map<String, String> parentMap = new HashMap<>();

        queue.add(sourceId);
        visited.add(sourceId);
        int nodesExplored = 0;
        boolean found = false;

        while (!queue.isEmpty()) {
            String current = queue.poll();
            nodesExplored++;

            if (current.equals(targetId)) {
                found = true;
                break;
            }

            for (String neighbor : graph.getNeighbors(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        if (!found) {
            return PathResult.unreachable(nodesExplored);
        }

        // Reconstruct shortest path backwards from targetId to sourceId
        LinkedList<String> path = new LinkedList<>();
        String curr = targetId;
        while (curr != null) {
            path.addFirst(curr);
            curr = parentMap.get(curr);
        }

        int degreesOfSeparation = path.size() - 1;
        return new PathResult(true, path, degreesOfSeparation, nodesExplored);
    }
}
