package com.example.gcm;

import java.util.concurrent.BlockingQueue;

/** Two daemon consumers share the failure queue and the same AttemptTracker. */
public final class FailureWorker extends Thread {
    private final BlockingQueue<FailedAttempt> queue;
    private final AttemptTracker tracker;

    public FailureWorker(String name, BlockingQueue<FailedAttempt> queue, AttemptTracker tracker) {
        super(name);
        this.queue = queue;
        this.tracker = tracker;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                FailedAttempt attempt = queue.take();
                LoginResult result = tracker.recordFailure(attempt.email());
                attempt.replyQueue().put(result);
            }
        } catch (InterruptedException e) {
            interrupt();
        }
    }
}
