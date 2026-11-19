# Verification performed in the preparation environment

Environment: Linux, Temurin JDK 25.0.1, JavaFX 21.0.6, Maven Wrapper 3.8.5.

- Maven test: 18 tests passed, zero failures/errors/skips. Covers invalid arguments and retry, policy boundary values, success reset, timed reset, unknown users, blocked password checks, queue replies, cancellation and preservation of Lab 1 output bytes.
- JavaFX smoke test using a headless Monocle display: actual FXML/controller interactions passed for disabled fields, error messages, shared state across two windows, countdown, re-enable, Welcome and the concurrency button.
- Runnable JAR packaging: Maven package succeeded; every manifest Class-Path entry resolves to its copied library. Invalid arguments and the real application lifecycle also passed using the packaged JAR.
- Actual LoginApplication lifecycle test: init/start/stop succeeded; closing a window during a 30-second block terminated the process with exit code 0.
- The required synchronized/unsynchronized experiment used the same sources. Only synchronized modifiers were removed from a temporary UserState copy. Synchronized: 9980 -> 9999 unblocked -> 10000 blocked. Unsynchronized observed run: 6718 -> 6737 unblocked -> 6738 unblocked. The exact race result varies.

These checks do not replace the course requirement for screenshots captured on the students' own computer. See the evidence logs in `verification/`. The headless test runtime is preparation tooling, not an application prerequisite.
