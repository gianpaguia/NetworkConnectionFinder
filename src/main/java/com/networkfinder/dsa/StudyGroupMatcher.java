package com.networkfinder.dsa;

import com.networkfinder.model.StudentProfile;

import java.util.*;

/**
 * Level-Order Breadth-First Search (BFS) to match students based on shared
 * profile attributes (e.g. subjects, study habits, sections) grouped by social distance.
 */
public class StudyGroupMatcher {

    /**
     * Traverses the graph level-by-level from currentStudentId to discover peers
     * matching academic and study criteria.
     */
    public static List<MatchResult> findMatches(Graph graph, String currentStudentId, MatchFilter filter) {
        List<MatchResult> matches = new ArrayList<>();
        if (graph == null || currentStudentId == null || !graph.containsVertex(currentStudentId)) {
            return matches;
        }

        StudentNode currentNode = graph.getNode(currentStudentId);
        StudentProfile currentProfile = currentNode.getProfile();
        if (currentProfile == null) {
            return matches;
        }

        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(currentStudentId);
        visited.add(currentStudentId);

        int currentDegree = 0;
        int maxDegree = (filter != null) ? filter.getMaxDegree() : Integer.MAX_VALUE;

        while (!queue.isEmpty() && currentDegree < maxDegree) {
            currentDegree++;
            int levelSize = queue.size();

            for (int i = 0; i < levelSize; i++) {
                String candidateId = queue.poll();

                for (String neighborId : graph.getNeighbors(candidateId)) {
                    if (!visited.contains(neighborId)) {
                        visited.add(neighborId);
                        queue.add(neighborId);

                        StudentNode neighborNode = graph.getNode(neighborId);
                        if (neighborNode == null || neighborNode.getProfile() == null) {
                            continue;
                        }

                        StudentProfile neighborProfile = neighborNode.getProfile();
                        MatchResult result = evaluateCompatibility(
                                currentNode, currentProfile, 
                                neighborNode, neighborProfile, 
                                currentDegree
                        );

                        if (matchesFilter(result, filter)) {
                            matches.add(result);
                        }
                    }
                }
            }
        }

        // Sort: primary by degree of separation (ascending), secondary by compatibility score (descending)
        matches.sort((a, b) -> {
            if (a.getDegreeOfSeparation() != b.getDegreeOfSeparation()) {
                return Integer.compare(a.getDegreeOfSeparation(), b.getDegreeOfSeparation());
            }
            return Integer.compare(b.getCompatibilityScore(), a.getCompatibilityScore());
        });

        return matches;
    }

    /**
     * Campus-wide peer discovery for unconnected students (degree 0 / isolated nodes),
     * evaluated and ranked by academic compatibility.
     */
    public static List<MatchResult> findCampusMatches(Graph graph, String currentStudentId, MatchFilter filter) {
        List<MatchResult> matches = new ArrayList<>();
        if (graph == null || currentStudentId == null || !graph.containsVertex(currentStudentId)) {
            return matches;
        }

        StudentNode currentNode = graph.getNode(currentStudentId);
        StudentProfile currentProfile = currentNode.getProfile();
        if (currentProfile == null) {
            return matches;
        }

        Set<String> directNeighbors = graph.getNeighbors(currentStudentId);

        for (StudentNode candidate : graph.getAllNodes()) {
            String candidateId = candidate.getStudentId();
            if (candidateId.equals(currentStudentId)) {
                continue;
            }
            // Skip students already directly connected
            if (directNeighbors.contains(candidateId)) {
                continue;
            }

            StudentProfile candidateProfile = candidate.getProfile();
            if (candidateProfile == null) {
                continue;
            }

            MatchResult result = evaluateCompatibility(
                    currentNode, currentProfile,
                    candidate, candidateProfile,
                    0
            );

            if (matchesFilter(result, filter)) {
                matches.add(result);
            }
        }

        // Sort descending by compatibility score
        matches.sort((a, b) -> Integer.compare(b.getCompatibilityScore(), a.getCompatibilityScore()));
        return matches;
    }

    private static MatchResult evaluateCompatibility(StudentNode node1, StudentProfile p1,
                                                      StudentNode node2, StudentProfile p2,
                                                      int degree) {
        List<String> commonSubjects = new ArrayList<>(p1.getSubjects());
        commonSubjects.retainAll(p2.getSubjects());

        List<String> commonHabits = new ArrayList<>(p1.getStudyHabits());
        commonHabits.retainAll(p2.getStudyHabits());

        boolean sameCourse = p1.getCourse() != null && p1.getCourse().equalsIgnoreCase(p2.getCourse());
        boolean sameSection = p1.getSection() != null && p1.getSection().equalsIgnoreCase(p2.getSection());
        boolean sameGroupSize = p1.getGroupSizePreference() != null && 
                                p1.getGroupSizePreference().equalsIgnoreCase(p2.getGroupSizePreference());

        // Scoring algorithm:
        // +25 per common subject
        // +15 per common study habit
        // +20 for exact section match
        // +15 for course match
        // +15 for matching group size preference
        int score = (commonSubjects.size() * 25)
                  + (commonHabits.size() * 15)
                  + (sameSection ? 20 : 0)
                  + (sameCourse ? 15 : 0)
                  + (sameGroupSize ? 15 : 0);

        return new MatchResult(
                node2, degree, commonSubjects, commonHabits, 
                sameSection, sameCourse, sameGroupSize, score
        );
    }

    private static boolean matchesFilter(MatchResult result, MatchFilter filter) {
        if (filter == null) {
            return true;
        }

        if (filter.isSameSectionOnly() && !result.isSameSection()) {
            return false;
        }

        if (filter.getRequiredSubject() != null && !filter.getRequiredSubject().isBlank()) {
            String targetSub = filter.getRequiredSubject().trim().toLowerCase();
            boolean hasSub = result.getStudent().getProfile().getSubjects().stream()
                    .anyMatch(s -> s.toLowerCase().contains(targetSub));
            if (!hasSub) return false;
        }

        if (filter.getRequiredStudyHabit() != null && !filter.getRequiredStudyHabit().isBlank()) {
            String targetHabit = filter.getRequiredStudyHabit().trim().toLowerCase();
            boolean hasHabit = result.getStudent().getProfile().getStudyHabits().stream()
                    .anyMatch(h -> h.toLowerCase().contains(targetHabit));
            if (!hasHabit) return false;
        }

        if (filter.getRequiredGroupSize() != null && !filter.getRequiredGroupSize().isBlank()) {
            String targetSize = filter.getRequiredGroupSize().trim().toLowerCase();
            String actualSize = result.getStudent().getProfile().getGroupSizePreference();
            if (actualSize == null || !actualSize.toLowerCase().contains(targetSize)) {
                return false;
            }
        }

        return true;
    }
}
