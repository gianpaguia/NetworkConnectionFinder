package com.networkfinder.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** A soft-deleted account snapshot plus the moment it was archived. */
public class ArchiveEntry {
    private final User user;
    private final StudentProfile profile;
    private final String archivedAt;

    public ArchiveEntry(User user, StudentProfile profile, String archivedAt) {
        this.user = user;
        this.profile = profile;
        this.archivedAt = archivedAt;
    }

    public User getUser() {
        return user;
    }

    public StudentProfile getProfile() {
        return profile;
    }

    public String getArchivedAt() {
        return archivedAt;
    }

    public String getArchivedAtDisplay() {
        if (archivedAt == null) {
            return "-";
        }
        try {
            return LocalDateTime.parse(archivedAt)
                    .format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"));
        } catch (Exception e) {
            return archivedAt;
        }
    }
}
