package com.networkfinder.service;

/**
 * Thrown when user-supplied input violates a validation rule enforced by
 * {@link InputValidator}. Carries a single, user-presentable message that
 * describes the first rule that was violated.
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }
}
