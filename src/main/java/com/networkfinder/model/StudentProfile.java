package com.networkfinder.model;

import java.util.ArrayList;
import java.util.List;

public class StudentProfile {
    private String studentId;
    private String course;
    private String yearLevel;
    private String section;
    private List<String> subjects = new ArrayList<>();
    private List<String> studyHabits = new ArrayList<>();
    private String groupSizePreference;
    private List<String> interests = new ArrayList<>();
    private String otherInterests;

    public StudentProfile() {
    }

    public StudentProfile(String studentId, String course, String yearLevel, String section,
                          List<String> subjects, List<String> studyHabits, 
                          String groupSizePreference, List<String> interests, String otherInterests) {
        this.studentId = studentId;
        this.course = course;
        this.yearLevel = yearLevel;
        this.section = section;
        if (subjects != null) this.subjects = new ArrayList<>(subjects);
        if (studyHabits != null) this.studyHabits = new ArrayList<>(studyHabits);
        this.groupSizePreference = groupSizePreference;
        if (interests != null) this.interests = new ArrayList<>(interests);
        this.otherInterests = otherInterests;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getYearLevel() {
        return yearLevel;
    }

    public void setYearLevel(String yearLevel) {
        this.yearLevel = yearLevel;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects != null ? subjects : new ArrayList<>();
    }

    public List<String> getStudyHabits() {
        return studyHabits;
    }

    public void setStudyHabits(List<String> studyHabits) {
        this.studyHabits = studyHabits != null ? studyHabits : new ArrayList<>();
    }

    public String getGroupSizePreference() {
        return groupSizePreference;
    }

    public void setGroupSizePreference(String groupSizePreference) {
        this.groupSizePreference = groupSizePreference;
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests != null ? interests : new ArrayList<>();
    }

    public String getOtherInterests() {
        return otherInterests;
    }

    public void setOtherInterests(String otherInterests) {
        this.otherInterests = otherInterests;
    }
}
