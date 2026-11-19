package com.example.gcm;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class LoginFlowTest {
    private static final String EMAIL = "hello@world.net";
    private static final String PASSWORD = "01041972^H";

    private LoginRuntime runtime(int n) throws InvalidUserException {
        return new LoginRuntime(new LoginConfig(n, 30), List.of(new User(EMAIL, PASSWORD)));
    }

    private LoginResult login(LoginRuntime runtime, String email, String password) throws Exception {
        BlockingQueue<LoginResult> reply = new ArrayBlockingQueue<>(1);
        Thread thread = new Thread(new LoginTask(email, password, runtime.service(), runtime.tracker(),
                runtime.queue(), reply::add, Runnable::run));
        try {
            thread.start();
            LoginResult result = reply.poll(3, TimeUnit.SECONDS);
            assertNotNull(result, "Login task did not return a result");
            thread.join(1000);
            assertFalse(thread.isAlive());
            return result;
        } finally {
            thread.interrupt();
        }
    }

    @Test void malformedPasswordsCountAndCorrectPasswordsCannotBypassABlock() throws Exception {
        try (LoginRuntime runtime = runtime(2)) {
            assertEquals(1, login(runtime, EMAIL, "x").remainingAttempts());
            assertEquals(LoginResult.Status.BLOCKED, login(runtime, EMAIL, "x").status());
            assertEquals(LoginResult.Status.BLOCKED, login(runtime, EMAIL, PASSWORD).status());
            assertEquals(2, runtime.tracker().failureCount(EMAIL));
        }
    }

    @Test void unknownEmailReturnsOnlyGenericErrorAndNoTracking() throws Exception {
        try (LoginRuntime runtime = runtime(3)) {
            LoginResult result = login(runtime, "unknown@example.org", PASSWORD);
            assertEquals("Incorrect details", result.message());
            assertEquals(1, runtime.tracker().trackedUserCount());
        }
    }

    @Test void successfulLoginResetsTheFailureCount() throws Exception {
        try (LoginRuntime runtime = runtime(3)) {
            login(runtime, EMAIL, "bad");
            assertEquals(LoginResult.Status.SUCCESS, login(runtime, EMAIL, PASSWORD).status());
            assertEquals(2, login(runtime, EMAIL, "bad").remainingAttempts());
        }
    }

    @Test void twoDaemonWorkersReturnEachEventsOwnResultWithoutLostUpdates() throws Exception {
        try (LoginRuntime runtime = runtime(100)) {
            assertEquals(2, runtime.workers().size());
            assertTrue(runtime.workers().stream().allMatch(Thread::isDaemon));
            List<FailedAttempt> events = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                FailedAttempt event = new FailedAttempt(EMAIL);
                events.add(event);
                runtime.queue().put(event);
            }
            Set<Integer> replies = new HashSet<>();
            for (FailedAttempt event : events) {
                LoginResult result = event.replyQueue().poll(3, TimeUnit.SECONDS);
                assertNotNull(result);
                replies.add(result.remainingAttempts());
            }
            assertEquals(20, replies.size());
            assertEquals(20, runtime.tracker().failureCount(EMAIL));
            assertTrue(replies.containsAll(List.of(80, 99)));
        }
    }

    @Test void interruptReleasesLoginWaitingForAReply() throws Exception {
        AttemptTracker tracker = new AttemptTracker(3, 10);
        tracker.registerUser(EMAIL);
        BlockingQueue<FailedAttempt> noWorkers = new LinkedBlockingQueue<>();
        BlockingQueue<LoginResult> results = new ArrayBlockingQueue<>(1);
        Thread login = new Thread(new LoginTask(EMAIL, "bad", new LoginService(List.of(
                new User(EMAIL, PASSWORD))), tracker, noWorkers, results::add, Runnable::run));
        try {
            login.start();
            assertNotNull(noWorkers.poll(3, TimeUnit.SECONDS));
            login.interrupt();
            login.join(1000);
            assertFalse(login.isAlive());
            assertTrue(results.isEmpty());
        } finally {
            login.interrupt();
        }
    }

    @Test void closingRuntimeStopsCountdownAndBothWorkers() throws Exception {
        LoginRuntime runtime = runtime(1);
        runtime.tracker().recordFailure(EMAIL);
        Thread countdown = runtime.threads().start("test-countdown", new CountdownTask(
                runtime.tracker(), EMAIL, seconds -> {}, () -> {}, Runnable::run));
        runtime.close();
        countdown.join(1000);
        assertFalse(countdown.isAlive());
        for (Thread worker : runtime.workers()) {
            worker.join(1000);
            assertFalse(worker.isAlive());
        }
        assertNull(runtime.threads().start("too-late", () -> fail("Started after shutdown")));
    }
}
