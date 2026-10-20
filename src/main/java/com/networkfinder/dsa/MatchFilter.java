package com.networkfinder.dsa;

public class MatchFilter {
    private String requiredSubject;
    private String requiredStudyHabit;
    private String requiredGroupSize;
    private boolean sameSectionOnly;
    private int maxDegree = Integer.MAX_VALUE;

    public MatchFilter() {
    }

    public String getRequiredSubject() {
        return requiredSubject;
    }

    public MatchFilter setRequiredSubject(String requiredSubject) {
        this.requiredSubject = requiredSubject;
        return this;
    }

    public String getRequiredStudyHabit() {
        return requiredStudyHabit;
    }

    public MatchFilter setRequiredStudyHabit(String requiredStudyHabit) {
        this.requiredStudyHabit = requiredStudyHabit;
        return this;
    }

    public String getRequiredGroupSize() {
        return requiredGroupSize;
    }

    public MatchFilter setRequiredGroupSize(String requiredGroupSize) {
        this.requiredGroupSize = requiredGroupSize;
        return this;
    }

    public boolean isSameSectionOnly() {
        return sameSectionOnly;
    }

    public MatchFilter setSameSectionOnly(boolean sameSectionOnly) {
        this.sameSectionOnly = sameSectionOnly;
        return this;
    }

    public int getMaxDegree() {
        return maxDegree;
    }

    public MatchFilter setMaxDegree(int maxDegree) {
        this.maxDegree = maxDegree;
        return this;
    }
}
