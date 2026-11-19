package com.example.gcm;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/** Loads files in init(), which JavaFX runs off its application thread. */
public class LoginApplication extends Application {
    private LoginRuntime runtime;
    private byte[] loginView;
    private byte[] welcomeView;
    private final Set<LoginController> controllers = new HashSet<>();
    private final Set<Stage> windows = new HashSet<>();

    @Override
    public void init() throws IOException {
        LoginConfig config = LoginConfig.parse(getParameters().getRaw().toArray(String[]::new));
        ArrayList<User> users;
        try (Scanner input = new Scanner(new File("users.txt"))) {
            users = UsersApp.readUsers(input);
        }
        loginView = readView("login-view.fxml");
        welcomeView = readView("welcome-view.fxml");
        runtime = new LoginRuntime(config, users);
    }

    private byte[] readView(String name) throws IOException {
        try (InputStream input = LoginApplication.class.getResourceAsStream(name)) {
            if (input == null) {
                throw new IOException("Missing view: " + name);
            }
            return input.readAllBytes();
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        showLogin(stage);
    }

    private void showLogin(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        // Files were read in init(); only UI construction happens here.
        stage.setScene(new Scene(loader.load(new ByteArrayInputStream(loginView))));
        LoginController controller = loader.getController();
        controller.configure(runtime, username -> showWelcome(stage, controller, username), () -> {
            try {
                showLogin(new Stage());
            } catch (IOException e) {
                throw new IllegalStateException("Cannot construct login view", e);
            }
        });
        controllers.add(controller);
        windows.add(stage);
        stage.setTitle("Users Login - Lab 3");
        stage.setOnCloseRequest(event -> shutDown());
        stage.show();
    }

    private void showWelcome(Stage stage, LoginController previous, String username) {
        try {
            FXMLLoader loader = new FXMLLoader();
            Scene scene = new Scene(loader.load(new ByteArrayInputStream(welcomeView)));
            WelcomeController controller = loader.getController();
            controller.setUsername(username);
            previous.dispose();
            controllers.remove(previous);
            stage.setTitle("Welcome");
            stage.setScene(scene);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot construct welcome view", e);
        }
    }

    private void shutDown() {
        controllers.forEach(LoginController::dispose);
        if (runtime != null) {
            runtime.close(); // Interrupts threads; never waits on the FX thread.
        }
        windows.forEach(Stage::hide);
        Platform.exit();
    }

    @Override
    public void stop() {
        controllers.forEach(LoginController::dispose);
        if (runtime != null) {
            runtime.close();
        }
    }
}
