package com.networkfinder.dsa;

import java.util.Collections;
import java.util.List;

public class MatchResult {
    private final StudentNode student;
    private final int degreeOfSeparation;
    private final List<String> commonSubjects;
    private final List<String> commonStudyHabits;
    private final boolean sameSection;
    private final boolean sameCourse;
    private final boolean sameGroupSize;
    private final int compatibilityScore;

    public MatchResult(StudentNode student, int degreeOfSeparation, List<String> commonSubjects,
                       List<String> commonStudyHabits, boolean sameSection, boolean sameCourse,
                       boolean sameGroupSize, int compatibilityScore) {
        this.student = student;
        this.degreeOfSeparation = degreeOfSeparation;
        this.commonSubjects = commonSubjects != null ? Collections.unmodifiableList(commonSubjects) : Collections.emptyList();
        this.commonStudyHabits = commonStudyHabits != null ? Collections.unmodifiableList(commonStudyHabits) : Collections.emptyList();
        this.sameSection = sameSection;
        this.sameCourse = sameCourse;
        this.sameGroupSize = sameGroupSize;
        this.compatibilityScore = compatibilityScore;
    }

    public StudentNode getStudent() {
        return student;
    }

    public int getDegreeOfSeparation() {
        return degreeOfSeparation;
    }

    public List<String> getCommonSubjects() {
        return commonSubjects;
    }

    public List<String> getCommonStudyHabits() {
        return commonStudyHabits;
    }

    public boolean isSameSection() {
        return sameSection;
    }

    public boolean isSameCourse() {
        return sameCourse;
    }

    public boolean isSameGroupSize() {
        return sameGroupSize;
    }

    public int getCompatibilityScore() {
        return compatibilityScore;
    }

    public String getDegreeLabel() {
        switch (degreeOfSeparation) {
            case 0: return "Campus Peer";
            case 1: return "Direct Connection (1st Degree)";
            case 2: return "Friend of Friend (2nd Degree)";
            case 3: return "3rd Degree Connection";
            default: return degreeOfSeparation + "th Degree Connection";
        }
    }
}
