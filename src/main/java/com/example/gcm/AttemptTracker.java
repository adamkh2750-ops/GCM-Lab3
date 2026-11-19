package com.example.gcm;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/** Shared by every login window. Only users loaded from the file are registered. */
public final class AttemptTracker {
    private final ConcurrentHashMap<String, UserState> states = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final int blockSeconds;
    private final LongSupplier clock;

    public AttemptTracker(int n, int t) {
        this(n, t, System::nanoTime);
    }

    AttemptTracker(int n, int t, LongSupplier clock) {
        new LoginConfig(n, t);
        maxFailures = n;
        blockSeconds = t;
        this.clock = clock;
    }

    public void registerUser(String email) {
        states.computeIfAbsent(email, key -> new UserState(maxFailures, blockSeconds, clock));
    }

    public boolean isKnownUser(String email) {
        return states.containsKey(email);
    }

    public LoginResult authenticate(String email, BooleanSupplier check) {
        UserState state = states.get(email);
        if (state == null) {
            check.getAsBoolean();
            return LoginResult.invalid();
        }
        return state.authenticate(email, check);
    }

    public LoginResult recordFailure(String email) {
        UserState state = states.get(email);
        return state == null ? LoginResult.invalid() : state.recordFailure();
    }

    public long remainingBlockSeconds(String email) {
        UserState state = states.get(email);
        return state == null ? 0 : state.remainingBlockSeconds();
    }

    public int failureCount(String email) {
        UserState state = states.get(email);
        return state == null ? 0 : state.failureCount();
    }

    public int trackedUserCount() {
        return states.size();
    }
}
