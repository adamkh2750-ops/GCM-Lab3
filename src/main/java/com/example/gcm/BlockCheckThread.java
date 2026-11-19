package com.example.gcm;

/** A short-lived Thread explicitly started and joined by LoginTask. */
public final class BlockCheckThread extends Thread {
    private final AttemptTracker tracker;
    private final String email;
    private LoginResult result;

    public BlockCheckThread(AttemptTracker tracker, String email) {
        super("block-check");
        this.tracker = tracker;
        this.email = email;
    }

    @Override
    public void run() {
        long remaining = tracker.remainingBlockSeconds(email);
        result = remaining > 0 ? LoginResult.blocked(remaining) : LoginResult.invalid();
    }

    /** Call only after join(), which also makes the result visible to the caller. */
    public LoginResult result() {
        return result;
    }
}
