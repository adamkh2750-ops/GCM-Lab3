# Lab 3 - draft report

Software Engineering 203.3140 | Winter 2026-27
Adam Kharbawi, Eyal Khatteb

**DRAFT: the implementation is prepared, but the branch must be published and the students must add screenshots captured on their own computer before submission.**


## 1. Repository and integration


Base repository: [github.com/eyalkhatteb/GCM-Labs](https://github.com/eyalkhatteb/GCM-Labs). The local lab3 branch starts from lab2 commit 0e4dd14. It has four implementation commits. Target URL after publication: [github.com/eyalkhatteb/GCM-Labs/tree/lab3](https://github.com/eyalkhatteb/GCM-Labs/tree/lab3). This target was not published during preparation.

Lab 1 User, UsersApp and InvalidUserException are reused unchanged. LoginService calls the User constructor for input validation and User.matches from Lab 2 for authentication. LoginTask catches InvalidUserException and treats malformed credentials as a failed login. The Lab 2 WelcomeController and welcome-view.fxml are retained.

Launcher reads the two positive integer arguments n and t before starting JavaFX. Invalid or missing arguments trigger repeated console prompts. LoginApplication.init loads users.txt and the FXML bytes off the FX thread. One LoginRuntime owns the shared tracking map, queue, two workers and cancellable background tasks for every login window.


## 2. Policy implemented


| Rule | Behavior |
| --- | --- |

| Known emails only | Only email addresses loaded from users.txt have tracking state. Unknown addresses receive the generic error and allocate no state. |

| Failure threshold | A known, unblocked account with an incorrect password gains one failure. Failure n starts a block lasting t seconds. |

| During a block | All attempts are rejected before checking credentials, including correct passwords. They neither increment the count nor extend the deadline. |

| Reset | Success resets failures. Block expiry resets failures and permits a new full cycle of n attempts. |

| Shared state | Windows in the same application process use the same per-email state. All data is in memory and disappears at exit. |

| Time and UI | Monotonic nanoTime deadlines prevent system-clock changes from affecting blocks. The countdown updates once per second off the FX thread. |


## 3. Classes and responsibilities


| Class | Responsibility |
| --- | --- |

| User | Lab 1 model: email/password validation and the Lab 2 matches method. |

| InvalidUserException | Checked exception for malformed credentials; reused from Lab 1. |

| UsersApp | Lab 1 input parser and sorted console output; reused without changes. |

| LoginConfig | Parses n and t and repeats the manual input prompt until valid or EOF. |

| LoginResult | Immutable success/error/blocked result, with the attempt count or remaining seconds. |

| UserState | One account monitor: failure count and block deadline. Every method is synchronized. |

| AttemptTracker | ConcurrentHashMap from registered email to UserState; shares policy across windows. |

| LoginService | Immutable user lookup that delegates validation and matching to the earlier labs. |

| FailedAttempt | Failure event containing an email and its own ArrayBlockingQueue of capacity 1 for the reply. |

| FailureWorker | Daemon Thread consuming failure events with take and replying with put. Exactly two exist. |

| BlockCheckThread | Short-lived Thread checking whether the email is currently blocked. |

| LoginTask | Runnable per login click: start/join the check thread, validate, put a failure event and take its reply, then dispatch the result. |

| CountdownTask | Runnable that sleeps in the background and dispatches countdown ticks and release to the UI. |

| BackgroundThreads | Tracks login/countdown/demo threads and interrupts them when the application closes. |

| LoginRuntime | Single process-wide owner of the configuration, service, tracker, queue, workers and tasks. |

| Launcher | Console argument validation followed by Application.launch; also the JAR entry point. |

| LoginApplication | Loads files in init, creates windows, opens Welcome and handles process-wide shutdown. |

| LoginController | Reads fields, disables/enables input, starts tasks and presents results; no failure/deadline policy. |

| WelcomeController | Unchanged Lab 2 controller displaying the authenticated email. |

| ConcurrencyDemo | The extra-button 20-thread experiment, run against separate test state with n=10000. |

login-view.fxml defines the login/test controls. welcome-view.fxml is the unchanged Lab 2 success screen. module-info.java opens the package to javafx.fxml. pom.xml retains JDK 25 / JavaFX 21.0.6, runs JUnit 5 tests and packages a runnable JAR with runtime libraries.


## 4. Required questions



### 1. Why must the JavaFX thread not wait t seconds?


JavaFX processes input, redraws and control updates on its application thread. Calling sleep, join or a blocking queue operation there prevents those events from being handled, so the window appears frozen. Our LoginTask performs joins and queue waits in a new background thread. CountdownTask sleeps in another background thread. Platform.runLater schedules only short UI updates on the JavaFX thread. The controller immediately disables the fields and Login while a login operation is running and re-enables them when its result arrives.


### 2. What can go wrong without synchronized in recordFailure?


Incrementing a shared integer is a read-modify-write operation. Two threads can both read the same old count, compute the same next value and write it back. One update is lost, so the account can receive too many failures before blocking. Checking the threshold and setting the deadline must also be part of the same atomic operation. A ConcurrentHashMap protects map operations; it does not protect mutable fields inside UserState. Synchronized methods use the same per-user monitor, serializing these state transitions while allowing different users to proceed independently. The measured comparison is in Section 5.


### 3. Why BlockingQueue rather than ArrayList with busy waiting?


The queue safely coordinates multiple producers and consumers. take waits without repeatedly polling when the queue is empty, and put waits when capacity is unavailable. This avoids wasting CPU on a busy loop and avoids writing our own locking/notification protocol around an ArrayList. Each FailedAttempt has a separate one-slot reply queue, so the LoginTask receives the result for its own failure even when the two workers complete events in a different order.


### 4. Why Runnable or Thread for each required role?


| Role | Reason |
| --- | --- |

| JavaFX application thread | Already supplied by JavaFX; it owns the controls and is not created by our code. |

| LoginTask - Runnable | Represents one piece of work, separate from the Thread that runs it. No inheritance from Thread is needed. |

| BlockCheckThread - extends Thread | The required short-lived check is explicitly started and joined by LoginTask. join also provides visibility of its result. |

| FailureWorker - extends Thread | Two named, long-lived consumer threads are explicitly managed and configured as daemons before starting. |

| CountdownTask - Runnable | Represents countdown work that can run in a separately managed cancellable Thread. Only dispatched callbacks touch controls. |


## 5. Verification and synchronization experiment


Preparation environment: Linux, Temurin JDK 25.0.1, JavaFX 21.0.6 and Maven Wrapper 3.8.5. These are actual preparation results; they are not claimed to be screenshots or runs from the students' Windows computer.

| Check | Observed result |
| --- | --- |

| Maven unit/integration tests | 18 passed; 0 failures, 0 errors, 0 skipped. Includes exact-byte preservation of the Lab 1 expected output. |

| JavaFX controls and FXML | Headless JavaFX smoke checks passed: input disabling, generic error, remaining attempts, two shared windows, blocking, release, Welcome and test button. |

| Shutdown and packaging | The real application init/start/stop flow exited with code 0 when a window closed during a 30-second block. The packaged JAR and manifest libraries were also checked. |

The required experiment starts 20 threads together. Each records 499 failures for the same test email: 9,980 in total. It then records 19 failures, followed by the final 20th failure. The GUI uses isolated test state with n=10,000 and the supplied t, leaving real user state untouched.


### With synchronized (actual result)


```text
20 threads x 499 failures; n=10000
After parallel work: 9980 (expected 9980)
After 19 more: 9999; blocked=false
After the 20th: 10000; blocked=true
PASS: no lost updates; exact blocking threshold.
```


### Without synchronized in a temporary copy (actual result)


```text
20 threads x 499 failures; n=10000
After parallel work: 6718 (expected 9980)
After 19 more: 6737; blocked=false
After the 20th: 6738; blocked=false
FAIL: updates were lost or the threshold was incorrect.
```

The unsynchronized copy lost 3,262 of the first 9,980 updates and did not block at the expected threshold. Only synchronized modifiers were removed from the temporary UserState copy. The submitted production sources remain synchronized. Race results depend on scheduling and can vary, including an occasional passing run. The supplied comparison scripts reproduce this experiment without changing the working sources.


## 6. Evidence still to add before submission


**The following evidence must be captured on the students' computer and added to the final PDF. This draft intentionally contains no fabricated local screenshots.**

| Required capture | How to obtain it |
| --- | --- |

| Invalid arguments | Run Launcher with bad or missing n/t; capture the console prompt, then enter a valid pair manually. |

| Successful login | Use hello@world.net / 01041972^H and capture the Welcome screen. |

| Remaining attempts | With n=3, enter an incorrect password once and capture the message showing 2 remaining attempts. |

| Countdown during block | Enter the third wrong password and capture the changing blocked-account message. |

| Automatic release | Wait t seconds and capture the cleared message and enabled Login button. |

| Both synchronization results | Run the supplied comparison script; capture the synchronized and unsynchronized outputs. |

| Commit history | After publishing lab3, capture the commit list and one commit detail/diff. |

| Process shutdown | Close a window during countdown and verify in the IDE that the process really stops. |


### Implementation commits already present locally


| Commit | Change |
| --- | --- |

| 88da25f | Add validated policy arguments and synchronized per-user attempt tracking |

| 8acbda7 | Implement threaded login with joined block checks and two queue workers |

| 4cc8eb3 | Connect responsive JavaFX screens, live countdown and concurrency demonstration |

| b3ad015 | Verify policy and concurrency, add runnable packaging and step-by-step setup guides |

To review commits after publication, select lab3 in GitHub and open the commits link, or use the repository path /commits/lab3. Confirm that the target branch is accessible to the grader before treating its link as final. The prepared commits are attributed to Codex; their author identity was not changed to impersonate the students.

After the branch link and required local evidence are complete, export the final report as **ID1_ID2.pdf**. Submit only that PDF through the course system; keep the complete compiling source project on GitHub.

References: supplied Lab3.pdf (3 pages) and the 16-slide threads deck; Lab 1 main / Lab 2 lab2 source; official Java Thread and BlockingQueue API, OpenJFX setup, IntelliJ program-argument documentation and GitHub repository-history documentation.
