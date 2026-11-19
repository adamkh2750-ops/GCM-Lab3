package com.example.gcm;

import javafx.application.Platform;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/** One login operation. Every potentially blocking operation runs off the FX thread. */
public final class LoginTask implements Runnable {
    private final String email;
    private final String password;
    private final LoginService service;
    private final AttemptTracker tracker;
    private final BlockingQueue<FailedAttempt> queue;
    private final Consumer<LoginResult> onResult;
    private final Consumer<Runnable> dispatch;

    public LoginTask(String email, String password, LoginService service, AttemptTracker tracker,
                     BlockingQueue<FailedAttempt> queue, Consumer<LoginResult> onResult) {
        this(email, password, service, tracker, queue, onResult, Platform::runLater);
    }

    // A dispatcher seam lets unit tests exercise the real flow without a display server.
    LoginTask(String email, String password, LoginService service, AttemptTracker tracker,
              BlockingQueue<FailedAttempt> queue, Consumer<LoginResult> onResult,
              Consumer<Runnable> dispatch) {
        this.email = email;
        this.password = password;
        this.service = service;
        this.tracker = tracker;
        this.queue = queue;
        this.onResult = onResult;
        this.dispatch = dispatch;
    }

    @Override
    public void run() {
        BlockCheckThread check = new BlockCheckThread(tracker, email);
        try {
            check.start();
            check.join();
            LoginResult result = check.result();
            if (result.status() != LoginResult.Status.BLOCKED) {
                result = tracker.authenticate(email, () -> {
                    try {
                        return service.authenticate(email, password);
                    } catch (InvalidUserException e) {
                        // Malformed input is still a failed attempt for an existing user.
                        return false;
                    }
                });
                if (result.status() == LoginResult.Status.ERROR && tracker.isKnownUser(email)) {
                    FailedAttempt attempt = new FailedAttempt(email);
                    queue.put(attempt);
                    result = attempt.replyQueue().take();
                }
            }
            if (!Thread.currentThread().isInterrupted()) {
                LoginResult completed = result;
                dispatch.accept(() -> onResult.accept(completed));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            check.interrupt();
        }
    }
}
