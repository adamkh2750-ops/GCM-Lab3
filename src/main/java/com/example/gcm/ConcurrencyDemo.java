package com.example.gcm;

import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/** The required 20 x 499 test, isolated from the real login accounts and policy. */
public final class ConcurrencyDemo implements Runnable {
    private final Consumer<String> onFinished;

    public ConcurrencyDemo(Consumer<String> onFinished) {
        this.onFinished = onFinished;
    }

    @Override
    public void run() {
        try {
            String output = experiment();
            System.out.println(output);
            Platform.runLater(() -> onFinished.accept(output));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Console entry point uses the same experiment as the button. */
    public static String experiment() throws InterruptedException {
        AttemptTracker tracker = new AttemptTracker(10_000, 30);
        String email = "concurrency@test.example";
        tracker.registerUser(email);
        CountDownLatch ready = new CountDownLatch(20);
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        try {
            for (int i = 0; i < 20; i++) {
                Thread thread = new Thread(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        for (int j = 0; j < 499 && !Thread.currentThread().isInterrupted(); j++) {
                            tracker.recordFailure(email);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, "counter-test-" + (i + 1));
                threads.add(thread);
                thread.start();
            }
            ready.await();
            start.countDown();
            for (Thread thread : threads) {
                thread.join();
            }
            int afterParallel = tracker.failureCount(email);
            boolean blockedEarly = tracker.remainingBlockSeconds(email) > 0;
            for (int i = 0; i < 19; i++) {
                tracker.recordFailure(email);
            }
            int after19 = tracker.failureCount(email);
            boolean blockedAfter19 = tracker.remainingBlockSeconds(email) > 0;
            LoginResult last = tracker.recordFailure(email);
            boolean blockedAfter20 = last.status() == LoginResult.Status.BLOCKED;
            boolean passed = afterParallel == 9_980 && !blockedEarly && after19 == 9_999
                    && !blockedAfter19 && blockedAfter20;
            return "20 threads x 499 failures; n=10000\n"
                    + "After parallel work: " + afterParallel + " (expected 9980)\n"
                    + "After 19 more: " + after19 + "; blocked=" + blockedAfter19 + "\n"
                    + "After the 20th: " + tracker.failureCount(email)
                    + "; blocked=" + blockedAfter20 + "\n"
                    + (passed ? "PASS: no lost updates; exact blocking threshold."
                    : "FAIL: updates were lost or the threshold was incorrect.");
        } finally {
            start.countDown();
            threads.forEach(Thread::interrupt);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println(experiment());
    }
}
