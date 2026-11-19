package com.example.gcm;

import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class AttemptTrackerTest {
    private static final String EMAIL = "hello@world.net";

    @Test void nthFailureBlocksAndAttemptsDuringBlockDoNotCountOrExtendIt() {
        AtomicLong clock = new AtomicLong();
        AttemptTracker tracker = new AttemptTracker(3, 10, clock::get);
        tracker.registerUser(EMAIL);
        assertEquals(2, tracker.recordFailure(EMAIL).remainingAttempts());
        assertEquals(1, tracker.recordFailure(EMAIL).remainingAttempts());
        assertEquals(LoginResult.Status.BLOCKED, tracker.recordFailure(EMAIL).status());
        clock.addAndGet(TimeUnit.SECONDS.toNanos(4));
        assertEquals(6, tracker.recordFailure(EMAIL).remainingSeconds());
        assertEquals(3, tracker.failureCount(EMAIL));
        assertEquals(6, tracker.remainingBlockSeconds(EMAIL));
    }

    @Test void evenCorrectCredentialsAreNotExaminedWhileBlocked() {
        AttemptTracker tracker = new AttemptTracker(1, 30);
        tracker.registerUser(EMAIL);
        tracker.recordFailure(EMAIL);
        AtomicBoolean checked = new AtomicBoolean();
        LoginResult result = tracker.authenticate(EMAIL, () -> { checked.set(true); return true; });
        assertEquals(LoginResult.Status.BLOCKED, result.status());
        assertFalse(checked.get());
    }

    @Test void expiryResetsCounterAndAllowsAnotherFullCycle() {
        AtomicLong clock = new AtomicLong();
        AttemptTracker tracker = new AttemptTracker(2, 3, clock::get);
        tracker.registerUser(EMAIL);
        tracker.recordFailure(EMAIL);
        tracker.recordFailure(EMAIL);
        clock.set(TimeUnit.SECONDS.toNanos(3) - 1);
        assertEquals(1, tracker.remainingBlockSeconds(EMAIL));
        clock.incrementAndGet();
        assertEquals(0, tracker.remainingBlockSeconds(EMAIL));
        assertEquals(0, tracker.failureCount(EMAIL));
        assertEquals(1, tracker.recordFailure(EMAIL).remainingAttempts());
        assertEquals(LoginResult.Status.BLOCKED, tracker.recordFailure(EMAIL).status());
    }

    @Test void successfulAuthenticationResetsOnlyThisUsersFailures() {
        AttemptTracker tracker = new AttemptTracker(3, 10);
        tracker.registerUser(EMAIL);
        tracker.registerUser("other@example.org");
        tracker.recordFailure(EMAIL);
        tracker.recordFailure("other@example.org");
        assertEquals(LoginResult.Status.SUCCESS, tracker.authenticate(EMAIL, () -> true).status());
        assertEquals(0, tracker.failureCount(EMAIL));
        assertEquals(1, tracker.failureCount("other@example.org"));
    }

    @Test void unknownEmailsNeverAllocateTrackingState() {
        AttemptTracker tracker = new AttemptTracker(1, 10);
        tracker.registerUser(EMAIL);
        for (int i = 0; i < 50; i++) {
            assertEquals(-1, tracker.recordFailure("unknown" + i).remainingAttempts());
        }
        assertEquals(1, tracker.trackedUserCount());
    }

    @Test void maximumIntegerDurationDoesNotOverflow() {
        AtomicLong clock = new AtomicLong();
        AttemptTracker tracker = new AttemptTracker(1, Integer.MAX_VALUE, clock::get);
        tracker.registerUser(EMAIL);
        assertEquals(Integer.MAX_VALUE, tracker.recordFailure(EMAIL).remainingSeconds());
    }

    @Test void twentyThreadsPreserveAll9980UpdatesAndBlockExactlyAt10000() throws Exception {
        assertTrue(ConcurrencyDemo.experiment(30).contains("PASS: no lost updates"));
    }
}
