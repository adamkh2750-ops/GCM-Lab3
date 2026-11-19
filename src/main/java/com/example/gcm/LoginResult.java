package com.example.gcm;

/** Immutable information passed from background threads to the controller. */
public record LoginResult(Status status, String username, int remainingAttempts,
                          long remainingSeconds) {
    public enum Status { SUCCESS, ERROR, BLOCKED }

    public static LoginResult success(String username) {
        return new LoginResult(Status.SUCCESS, username, -1, 0);
    }

    /** -1 deliberately omits an attempt count for unknown email addresses. */
    public static LoginResult invalid() {
        return invalid(-1);
    }

    public static LoginResult invalid(int remainingAttempts) {
        return new LoginResult(Status.ERROR, "", remainingAttempts, 0);
    }

    public static LoginResult blocked(long seconds) {
        return new LoginResult(Status.BLOCKED, "", -1, seconds);
    }

    public String message() {
        return switch (status) {
            case SUCCESS -> "Login successful";
            case ERROR -> remainingAttempts < 0 ? "Incorrect details"
                    : "Incorrect details. Attempts remaining: " + remainingAttempts;
            case BLOCKED -> "Account blocked. " + remainingSeconds + " seconds remaining";
        };
    }
}
