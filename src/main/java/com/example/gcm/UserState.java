package com.example.gcm;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/** One email's monitor. Every method is synchronized on this same object. */
public final class UserState {
    private final int maxFailures;
    private final long blockDurationNanos;
    private final LongSupplier clock;
    private int failures;
    private boolean blocked;
    private long blockedUntilNanos;

    public UserState(int maxFailures, int blockSeconds) {
        this(maxFailures, blockSeconds, System::nanoTime);
    }

    UserState(int maxFailures, int blockSeconds, LongSupplier clock) {
        new LoginConfig(maxFailures, blockSeconds);
        this.maxFailures = maxFailures;
        this.blockDurationNanos = TimeUnit.SECONDS.toNanos(blockSeconds);
        this.clock = clock;
    }

    /** Rechecks the block while holding the lock, before even checking credentials. */
    public synchronized LoginResult authenticate(String username, BooleanSupplier check) {
        expireBlock();
        if (blocked) {
            return LoginResult.blocked(remainingBlockSeconds());
        }
        if (check.getAsBoolean()) {
            failures = 0;
            return LoginResult.success(username);
        }
        // Only FailureWorker records failures; authentication does not increment here.
        return LoginResult.invalid();
    }

    public synchronized LoginResult recordFailure() {
        expireBlock();
        if (blocked) {
            return LoginResult.blocked(remainingBlockSeconds());
        }
        failures++;
        if (failures == maxFailures) {
            blockedUntilNanos = clock.getAsLong() + blockDurationNanos;
            blocked = true;
            return LoginResult.blocked(remainingBlockSeconds());
        }
        return LoginResult.invalid(maxFailures - failures);
    }

    public synchronized int failureCount() {
        expireBlock();
        return failures;
    }

    public synchronized boolean isBlocked() {
        expireBlock();
        return blocked;
    }

    public synchronized long remainingBlockSeconds() {
        expireBlock();
        if (!blocked) {
            return 0;
        }
        long remaining = blockedUntilNanos - clock.getAsLong();
        // A positive fraction of a second must still display as 1, never as 0.
        return remaining <= 0 ? 0 : 1 + (remaining - 1) / 1_000_000_000L;
    }

    private synchronized void expireBlock() {
        // nanoTime is monotonic; changing the computer's clock cannot extend a block.
        if (blocked && blockedUntilNanos - clock.getAsLong() <= 0) {
            blocked = false;
            failures = 0;
        }
    }
}
