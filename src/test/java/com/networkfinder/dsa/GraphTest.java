package com.networkfinder.dsa;

import com.networkfinder.model.StudentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class GraphTest {
    private Graph graph;

    @BeforeEach
    public void setUp() {
        graph = new Graph();
        StudentProfile p1 = new StudentProfile("2026-001", "BSIT", "1st Year", "BSIT 1A",
                List.of("Programming", "Data Structures"), List.of("Coding Together"), "Small Group",
                List.of("Technology"), "");
        StudentProfile p2 = new StudentProfile("2026-002", "BSIT", "1st Year", "BSIT 1A",
                List.of("Programming", "Networking"), List.of("Coding Together"), "Small Group",
                List.of("Gaming"), "");
        StudentProfile p3 = new StudentProfile("2026-003", "BSCS", "2nd Year", "BSCS 2A",
                List.of("Mathematics", "Algorithms"), List.of("Solving Problems"), "Medium Group",
                List.of("Research"), "");

        graph.addVertex(new StudentNode("2026-001", "Juan Dela Cruz", p1));
        graph.addVertex(new StudentNode("2026-002", "Maria Santos", p2));
        graph.addVertex(new StudentNode("2026-003", "Mark Reyes", p3));
    }

    @Test
    public void testAddVertices() {
        assertEquals(3, graph.getVertexCount());
        assertTrue(graph.containsVertex("2026-001"));
        assertTrue(graph.containsVertex("2026-002"));
        assertTrue(graph.containsVertex("2026-003"));
        assertFalse(graph.containsVertex("non-existent"));
    }

    @Test
    public void testAddAndRemoveEdge() {
        assertTrue(graph.addEdge("2026-001", "2026-002"));
        assertEquals(1, graph.getEdgeCount());
        assertTrue(graph.hasEdge("2026-001", "2026-002"));
        assertTrue(graph.hasEdge("2026-002", "2026-001"));

        Set<String> neighbors1 = graph.getNeighbors("2026-001");
        assertTrue(neighbors1.contains("2026-002"));
        assertEquals(1, graph.getDegree("2026-001"));

        assertTrue(graph.removeEdge("2026-001", "2026-002"));
        assertEquals(0, graph.getEdgeCount());
        assertFalse(graph.hasEdge("2026-001", "2026-002"));
        assertEquals(0, graph.getDegree("2026-001"));
    }

    @Test
    public void testSelfEdgeRejected() {
        assertFalse(graph.addEdge("2026-001", "2026-001"));
        assertEquals(0, graph.getEdgeCount());
    }

    @Test
    public void testRemoveVertexCascadesToEdges() {
        graph.addEdge("2026-001", "2026-002");
        graph.addEdge("2026-001", "2026-003");
        assertEquals(2, graph.getEdgeCount());
        assertEquals(2, graph.getDegree("2026-001"));
        assertEquals(1, graph.getDegree("2026-002"));

        // Remove vertex 2026-001
        assertTrue(graph.removeVertex("2026-001"));
        assertEquals(2, graph.getVertexCount());
        assertFalse(graph.containsVertex("2026-001"));

        // Incident edges to 2026-001 must be completely removed from 2026-002 and 2026-003
        assertEquals(0, graph.getEdgeCount());
        assertEquals(0, graph.getDegree("2026-002"));
        assertEquals(0, graph.getDegree("2026-003"));
    }
}
