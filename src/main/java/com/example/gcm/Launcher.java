package com.example.gcm;

import javafx.application.Application;
import java.util.Scanner;

/** Validates arguments in the console before the first JavaFX window exists. */
public class Launcher {
    public static void main(String[] args) {
        LoginConfig config;
        try (Scanner input = new Scanner(System.in)) {
            config = LoginConfig.read(args, input, System.out);
        }
        if (config != null) {
            Application.launch(LoginApplication.class,
                    Integer.toString(config.maxFailures()), Integer.toString(config.blockSeconds()));
        }
    }
}
