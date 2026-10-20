package com.networkfinder.service;

// Immutable outcome of a service mutation: ok flag plus a human-readable reason.
public class OperationResult {
    private final boolean ok;
    private final String message;

    private OperationResult(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public static OperationResult ok() {
        return new OperationResult(true, "");
    }

    public static OperationResult ok(String message) {
        return new OperationResult(true, message);
    }

    public static OperationResult fail(String message) {
        return new OperationResult(false, message);
    }

    public boolean isOk() {
        return ok;
    }

    public String getMessage() {
        return message;
    }
}
