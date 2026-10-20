package com.networkfinder.dsa;

import com.networkfinder.model.StudentProfile;

public class StudentNode {
    private final String studentId;
    private String fullName;
    private StudentProfile profile;

    public StudentNode(String studentId, String fullName, StudentProfile profile) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.profile = profile;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public StudentProfile getProfile() {
        return profile;
    }

    public void setProfile(StudentProfile profile) {
        this.profile = profile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentNode that = (StudentNode) o;
        return studentId.equals(that.studentId);
    }

    @Override
    public int hashCode() {
        return studentId.hashCode();
    }

    @Override
    public String toString() {
        return fullName + " [" + studentId + "]";
    }
}
