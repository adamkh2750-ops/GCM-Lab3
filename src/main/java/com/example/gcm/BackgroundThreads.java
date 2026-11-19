package com.example.gcm;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Owns cancellable login, countdown and demonstration threads for the application. */
public final class BackgroundThreads implements AutoCloseable {
    private final Set<Thread> running = ConcurrentHashMap.newKeySet();
    private boolean closed;

    public synchronized Thread start(String name, Runnable work) {
        if (closed) {
            return null;
        }
        Thread thread = new Thread(() -> {
            try {
                work.run();
            } finally {
                running.remove(Thread.currentThread());
            }
        }, name);
        running.add(thread);
        thread.start();
        return thread;
    }

    /** Interrupt only: the JavaFX thread must never wait in join(). */
    @Override
    public synchronized void close() {
        closed = true;
        running.forEach(Thread::interrupt);
    }

    public Set<Thread> runningThreads() {
        return Set.copyOf(running);
    }
}
