package com.example.gcm;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/** One event and its own one-element reply channel, so replies cannot be mixed up. */
public final class FailedAttempt {
    private final String email;
    private final BlockingQueue<LoginResult> reply = new ArrayBlockingQueue<>(1);

    public FailedAttempt(String email) {
        this.email = email;
    }

    public String email() {
        return email;
    }

    public BlockingQueue<LoginResult> replyQueue() {
        return reply;
    }
}
