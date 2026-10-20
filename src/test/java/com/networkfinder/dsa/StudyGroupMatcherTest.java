package com.networkfinder.dsa;

import com.networkfinder.model.StudentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StudyGroupMatcherTest {
    private Graph graph;

    @BeforeEach
    public void setUp() {
        graph = new Graph();
        // Setup: Current user is S1
        // S1 connects to S2 (1st degree)
        // S2 connects to S3 (2nd degree)
        // S3 connects to S4 (3rd degree)

        StudentProfile p1 = new StudentProfile("S1", "BSIT", "1st Year", "BSIT 1A",
                List.of("Programming", "Data Structures"), List.of("Coding Together"), "Small Group",
                List.of("Technology"), "");
        StudentProfile p2 = new StudentProfile("S2", "BSIT", "1st Year", "BSIT 1A",
                List.of("Programming", "Networking"), List.of("Coding Together"), "Small Group",
                List.of("Gaming"), "");
        StudentProfile p3 = new StudentProfile("S3", "BSIT", "1st Year", "BSIT 1B",
                List.of("Programming", "Data Structures"), List.of("Solving Problems"), "Medium Group",
                List.of("Sports"), "");
        StudentProfile p4 = new StudentProfile("S4", "BSCS", "2nd Year", "BSCS 2A",
                List.of("Mathematics"), List.of("Exam Review"), "Large Group",
                List.of("Music"), "");

        graph.addVertex(new StudentNode("S1", "Student One", p1));
        graph.addVertex(new StudentNode("S2", "Student Two", p2));
        graph.addVertex(new StudentNode("S3", "Student Three", p3));
        graph.addVertex(new StudentNode("S4", "Student Four", p4));

        graph.addEdge("S1", "S2");
        graph.addEdge("S2", "S3");
        graph.addEdge("S3", "S4");
    }

    @Test
    public void testLevelOrderTraversals() {
        List<MatchResult> matches = StudyGroupMatcher.findMatches(graph, "S1", null);
        assertEquals(3, matches.size());

        // First match should be 1st degree S2
        assertEquals("S2", matches.get(0).getStudent().getStudentId());
        assertEquals(1, matches.get(0).getDegreeOfSeparation());

        // Second match should be 2nd degree S3
        assertEquals("S3", matches.get(1).getStudent().getStudentId());
        assertEquals(2, matches.get(1).getDegreeOfSeparation());

        // Third match should be 3rd degree S4
        assertEquals("S4", matches.get(2).getStudent().getStudentId());
        assertEquals(3, matches.get(2).getDegreeOfSeparation());
    }

    @Test
    public void testFilteringBySubject() {
        MatchFilter filter = new MatchFilter().setRequiredSubject("Data Structures");
        List<MatchResult> matches = StudyGroupMatcher.findMatches(graph, "S1", filter);

        // S3 shares Data Structures
        assertEquals(1, matches.size());
        assertEquals("S3", matches.get(0).getStudent().getStudentId());
    }

    @Test
    public void testFilteringBySameSection() {
        MatchFilter filter = new MatchFilter().setSameSectionOnly(true);
        List<MatchResult> matches = StudyGroupMatcher.findMatches(graph, "S1", filter);

        // Only S2 is in BSIT 1A
        assertEquals(1, matches.size());
        assertEquals("S2", matches.get(0).getStudent().getStudentId());
    }

    @Test
    public void testCampusMatchesForIsolatedStudent() {
        // Create an isolated student S_ISO with no edges
        StudentProfile pIso = new StudentProfile("S_ISO", "BSIT", "1st Year", "BSIT 1A",
                List.of("Programming", "Data Structures"), List.of("Coding Together"), "Small Group",
                List.of("Technology"), "");
        graph.addVertex(new StudentNode("S_ISO", "Angelo Alon", pIso));

        // BFS traversal returns 0 matches because degree is 0
        List<MatchResult> bfsMatches = StudyGroupMatcher.findMatches(graph, "S_ISO", null);
        assertTrue(bfsMatches.isEmpty(), "BFS matches should be empty for isolated cold-start student");

        // Campus match discovery returns all other students ranked by compatibility
        List<MatchResult> campusMatches = StudyGroupMatcher.findCampusMatches(graph, "S_ISO", null);
        assertEquals(4, campusMatches.size(), "Should discover all 4 campus peers");

        // Top match should be S1 or S2 (both sharing subjects, section, and course)
        assertTrue(campusMatches.get(0).getCompatibilityScore() > 0);
        assertEquals(0, campusMatches.get(0).getDegreeOfSeparation());
        assertEquals("Campus Peer", campusMatches.get(0).getDegreeLabel());
    }
}
