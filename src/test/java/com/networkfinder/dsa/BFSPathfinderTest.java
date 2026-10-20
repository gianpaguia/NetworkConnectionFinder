package com.networkfinder.dsa;

import com.networkfinder.model.StudentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BFSPathfinderTest {
    private Graph graph;

    @BeforeEach
    public void setUp() {
        graph = new Graph();
        // Construct path: A -- B -- C -- D and isolated E
        String[] ids = {"A", "B", "C", "D", "E"};
        for (String id : ids) {
            graph.addVertex(new StudentNode(id, "Student " + id, new StudentProfile()));
        }

        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        graph.addEdge("C", "D");
    }

    @Test
    public void testDirectConnection() {
        PathResult result = BFSPathfinder.findShortestPath(graph, "A", "B");
        assertTrue(result.isReachable());
        assertEquals(1, result.getDegreesOfSeparation());
        assertEquals(List.of("A", "B"), result.getPath());
    }

    @Test
    public void testMultiHopConnection() {
        PathResult result = BFSPathfinder.findShortestPath(graph, "A", "D");
        assertTrue(result.isReachable());
        assertEquals(3, result.getDegreesOfSeparation());
        assertEquals(List.of("A", "B", "C", "D"), result.getPath());
    }

    @Test
    public void testShortestPathWithAlternativeLoop() {
        // Add shortcut A -- C
        graph.addEdge("A", "C");
        PathResult result = BFSPathfinder.findShortestPath(graph, "A", "D");
        assertTrue(result.isReachable());
        // Shortest path should now be A -> C -> D (2 degrees of separation)
        assertEquals(2, result.getDegreesOfSeparation());
        assertEquals(List.of("A", "C", "D"), result.getPath());
    }

    @Test
    public void testUnreachableTarget() {
        PathResult result = BFSPathfinder.findShortestPath(graph, "A", "E");
        assertFalse(result.isReachable());
        assertEquals(-1, result.getDegreesOfSeparation());
        assertTrue(result.getPath().isEmpty());
    }

    @Test
    public void testSameSourceAndTarget() {
        PathResult result = BFSPathfinder.findShortestPath(graph, "A", "A");
        assertTrue(result.isReachable());
        assertEquals(0, result.getDegreesOfSeparation());
        assertEquals(List.of("A"), result.getPath());
    }
}
