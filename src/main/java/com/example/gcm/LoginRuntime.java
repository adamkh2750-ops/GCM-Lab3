package com.example.gcm;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** One runtime per process, shared across windows; all state disappears on exit. */
public final class LoginRuntime implements AutoCloseable {
    private final LoginConfig config;
    private final LoginService service;
    private final AttemptTracker tracker;
    private final BlockingQueue<FailedAttempt> queue = new LinkedBlockingQueue<>();
    private final BackgroundThreads threads = new BackgroundThreads();
    private final List<FailureWorker> workers;

    public LoginRuntime(LoginConfig config, List<User> users) {
        this.config = config;
        service = new LoginService(users);
        tracker = new AttemptTracker(config.maxFailures(), config.blockSeconds());
        service.usernames().forEach(tracker::registerUser);
        workers = List.of(new FailureWorker("failure-worker-1", queue, tracker),
                new FailureWorker("failure-worker-2", queue, tracker));
        workers.forEach(Thread::start);
    }

    public LoginConfig config() { return config; }
    public LoginService service() { return service; }
    public AttemptTracker tracker() { return tracker; }
    public BlockingQueue<FailedAttempt> queue() { return queue; }
    public BackgroundThreads threads() { return threads; }
    public List<FailureWorker> workers() { return workers; }

    @Override
    public void close() {
        threads.close();
        workers.forEach(Thread::interrupt);
    }
}
