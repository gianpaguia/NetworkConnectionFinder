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

    public OperationResult approveStudent(String studentId) {
        if (userDao.updateVerificationStatus(studentId, VerificationStatus.APPROVED)) {
            return OperationResult.ok("Registration approved.");
        }
        return OperationResult.fail("Could not approve — the registration request may have already been processed.");
    }

    public OperationResult rejectStudent(String studentId) {
        if (userDao.updateVerificationStatus(studentId, VerificationStatus.REJECTED)) {
            return OperationResult.ok("Registration rejected.");
        }
        return OperationResult.fail("Could not reject — the registration request may have already been processed.");
    }
}
