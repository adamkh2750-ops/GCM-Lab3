package com.example.gcm;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.util.function.Consumer;

/** Reads fields, starts tasks and presents results. Policy and timing stay in the model. */
public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Label policyLabel;
    @FXML private Button loginButton;
    @FXML private Button testButton;
    @FXML private TextArea testOutput;

    private LoginRuntime runtime;
    private Consumer<String> onWelcome;
    private Runnable openAnotherWindow;
    private Thread countdownThread;
    private Thread testThread;
    private boolean busy;
    private boolean disposed;
    private long viewVersion;

    public void configure(LoginRuntime runtime, Consumer<String> onWelcome, Runnable openAnotherWindow) {
        this.runtime = runtime;
        this.onWelcome = onWelcome;
        this.openAnotherWindow = openAnotherWindow;
        policyLabel.setText("Limit: " + runtime.config().maxFailures()
                + " failed attempts. Block: " + runtime.config().blockSeconds() + " seconds.");
        usernameField.textProperty().addListener((observable, before, after) -> {
            if (!busy) {
                cancelCountdown();
                errorLabel.setText("");
                loginButton.setDisable(false);
            }
        });
    }

    @FXML
    void onLoginButtonClick() {
        if (busy || disposed) {
            return;
        }
        cancelCountdown();
        String email = usernameField.getText().trim();
        String password = passwordField.getText();
        long version = ++viewVersion;
        setBusy(true);
        errorLabel.setText("בודק פרטים...");
        runtime.threads().start("login-task", new LoginTask(email, password, runtime.service(),
                runtime.tracker(), runtime.queue(), result -> {
                    if (!disposed && viewVersion == version) {
                        showResult(email, result);
                    }
                }));
    }

    private void showResult(String email, LoginResult result) {
        setBusy(false);
        errorLabel.setText(result.message());
        switch (result.status()) {
            case SUCCESS -> onWelcome.accept(result.username());
            case ERROR -> { /* The result already contains the remaining attempt count. */ }
            case BLOCKED -> {
                loginButton.setDisable(true);
                long version = ++viewVersion;
                countdownThread = runtime.threads().start("countdown", new CountdownTask(
                        runtime.tracker(), email,
                        seconds -> {
                            if (!disposed && viewVersion == version) {
                                errorLabel.setText(LoginResult.blocked(seconds).message());
                            }
                        },
                        () -> {
                            if (!disposed && viewVersion == version) {
                                errorLabel.setText("");
                                loginButton.setDisable(false);
                                countdownThread = null;
                            }
                        }));
            }
        }
    }

    private void setBusy(boolean value) {
        busy = value;
        usernameField.setDisable(value);
        passwordField.setDisable(value);
        loginButton.setDisable(value);
    }

    private void cancelCountdown() {
        viewVersion++;
        if (countdownThread != null) {
            countdownThread.interrupt();
            countdownThread = null;
        }
    }

    @FXML
    void onTestButtonClick() {
        if (disposed || testThread != null && testThread.isAlive()) {
            return;
        }
        testButton.setDisable(true);
        testOutput.setText("Running 20 threads against one separate test account...");
        testThread = runtime.threads().start("synchronization-demo", new ConcurrencyDemo(
                runtime.config().blockSeconds(),
                text -> {
                    if (!disposed) {
                        testOutput.setText(text);
                        testButton.setDisable(false);
                    }
                }));
    }

    @FXML
    void onNewWindowClick() {
        if (!disposed) {
            openAnotherWindow.run();
        }
    }

    public void dispose() {
        disposed = true;
        cancelCountdown();
        if (testThread != null) {
            testThread.interrupt();
        }
    }
}
