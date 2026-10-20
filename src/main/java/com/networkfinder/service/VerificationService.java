package com.networkfinder.service;

import com.networkfinder.database.UserDao;
import com.networkfinder.model.User;
import com.networkfinder.model.VerificationStatus;

import java.util.List;

public class VerificationService {
    private final UserDao userDao = new UserDao();

    public List<User> getPendingVerifications() {
        return userDao.getPendingStudents();
    }

    public boolean approveStudent(String studentId) {
        return userDao.updateVerificationStatus(studentId, VerificationStatus.APPROVED);
    }

    public boolean rejectStudent(String studentId) {
        return userDao.updateVerificationStatus(studentId, VerificationStatus.REJECTED);
    }
}
