package com.example.gcm;

import javafx.application.Platform;

import java.util.function.Consumer;

/** Sleeps in a background thread and asks the model, not the controller, for time. */
public final class CountdownTask implements Runnable {
    private final AttemptTracker tracker;
    private final String email;
    private final Consumer<Long> onTick;
    private final Runnable onFinished;
    private final Consumer<Runnable> dispatch;

    public CountdownTask(AttemptTracker tracker, String email, Consumer<Long> onTick,
                         Runnable onFinished) {
        this(tracker, email, onTick, onFinished, Platform::runLater);
    }

    CountdownTask(AttemptTracker tracker, String email, Consumer<Long> onTick,
                  Runnable onFinished, Consumer<Runnable> dispatch) {
        this.tracker = tracker;
        this.email = email;
        this.onTick = onTick;
        this.onFinished = onFinished;
        this.dispatch = dispatch;
    }

    @Override
    public void run() {
        try {
            long previous = -1;
            while (!Thread.currentThread().isInterrupted()) {
                long seconds = tracker.remainingBlockSeconds(email);
                if (seconds == 0) {
                    dispatch.accept(onFinished);
                    return;
                }
                if (seconds != previous) {
                    dispatch.accept(() -> onTick.accept(seconds));
                    previous = seconds;
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
