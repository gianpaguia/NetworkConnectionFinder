package com.networkfinder.dsa;

import com.networkfinder.model.StudentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkMetricsTest {
    private Graph graph;

    @BeforeEach
    public void setUp() {
        graph = new Graph();
        for (String id : List.of("A", "B", "C", "D")) {
            graph.addVertex(new StudentNode(id, "Student " + id, new StudentProfile()));
        }

        // Mutual setup:
        // A is connected to C and D
        // B is connected to C and D
        graph.addEdge("A", "C");
        graph.addEdge("A", "D");
        graph.addEdge("B", "C");
        graph.addEdge("B", "D");
    }

    @Test
    public void testMutualConnections() {
        List<String> mutuals = NetworkMetrics.getMutualConnections(graph, "A", "B");
        assertEquals(2, mutuals.size());
        assertTrue(mutuals.contains("C"));
        assertTrue(mutuals.contains("D"));
    }

    @Test
    public void testDegreeCentrality() {
        // N = 4 students, A has 2 edges -> 2 / (4 - 1) = 2/3 = ~0.6667
        double centrality = NetworkMetrics.getDegreeCentrality(graph, "A");
        assertEquals(2.0 / 3.0, centrality, 0.001);
    }
}
