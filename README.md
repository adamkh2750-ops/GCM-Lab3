# GCM - Lab 3: threaded login

Continuation of `lab2` from `https://github.com/eyalkhatteb/GCM-Labs`.

Open `docs/Lab3_Step_By_Step_HE.html` in a browser for the Hebrew setup, test and submission guide.

## Requirements and run

Use JDK 25. Maven resolves JavaFX 21.0.6. Threads and queues are part of the JDK.

In IntelliJ, open `pom.xml`, reload Maven, and run `com.example.gcm.Launcher` with program arguments `3 10` and the project root as the working directory. The portable `Lab3` run configuration supplies these example arguments. Missing or invalid arguments are requested again in the console before JavaFX starts.

Windows PowerShell, from the project root with JAVA_HOME pointing to JDK 25:

```powershell
.\mvnw.cmd test
.\mvnw.cmd javafx:run "-Djavafx.args=3 10"
.\mvnw.cmd package
java -jar target/GCM-1.0-SNAPSHOT.jar 3 10
```

Linux/macOS use `bash ./mvnw` instead of `.\mvnw.cmd`. The JAR uses the `target/lib` directory created by `package`; keep it next to the JAR, and run from the root containing `users.txt`. Build on the target operating system so Maven selects its JavaFX native libraries.

A valid sample login is `hello@world.net` / `01041972^H`.

## Concurrency and policy

`LoginTask` implements `Runnable`. It starts and joins `BlockCheckThread`, delegates validation to the Lab 1 `User` through `LoginService`, and submits known users' failures to a shared blocking queue. Exactly two daemon `FailureWorker` threads consume events. Every event has its own one-element `ArrayBlockingQueue` reply channel.

All `UserState` methods are synchronized. The `ConcurrentHashMap` in `AttemptTracker` stores states only for usernames loaded from the input. A successful login resets the count. Attempts during a block are rejected without checking passwords, incrementing the count or extending the deadline. Expiry resets the count. One `LoginRuntime` is shared by all windows in this process. Separate application processes have separate in-memory states.

`CountdownTask` sleeps off the FX thread. UI updates are dispatched with `Platform.runLater`. File reads happen in `Application.init()`, off the FX thread; even the FXML bytes are preloaded. The controller contains no failure-count or deadline policy. Closing any window disposes controllers, interrupts all managed tasks and both workers, and exits JavaFX without joining on the UI thread.

## Required synchronization experiment

The login-screen test button runs 20 concurrent threads with 499 failures each against one isolated account and n=10000. It checks 9980, then 9999 without blocking, then 10000 with blocking. The block duration comes from the supplied application policy.

Compare the same experiment with and without the `synchronized` modifiers using temporary source copies:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\compare-synchronization.ps1 -BlockSeconds 10
```

Linux:

```sh
bash scripts/compare-synchronization.sh 10
```

The scripts leave production sources unchanged. Races are nondeterministic; an unsynchronized run can occasionally pass.

## Submission status

Adam and Eyal share the Lab 2 base at commit `0e4dd14`. This copy contains the Lab 3 version for Adam’s GitHub account, `adamkh2750-ops`. Set the upload destination to Adam’s own repository using step 9 of the Hebrew guide. The PDF lists both students and links to the `lab3` branch in Adam’s repository. Its submission filename is `ID1_ID2.pdf`.

The report and local verification evidence are described in `docs/report-draft.md` and `docs/verification.md`. A draft is not a final submission.
