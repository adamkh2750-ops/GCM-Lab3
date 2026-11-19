package com.example.gcm;

import java.io.PrintStream;
import java.util.Scanner;

/** The two policy values supplied by the user, never production defaults. */
public record LoginConfig(int maxFailures, int blockSeconds) {
    public LoginConfig {
        if (maxFailures < 1 || blockSeconds < 1) {
            throw new IllegalArgumentException("n and t must both be at least 1.");
        }
    }

    public static LoginConfig parse(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected exactly two arguments: n t.");
        }
        try {
            return new LoginConfig(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("n and t must be whole numbers.");
        }
    }

    /** Runs before JavaFX starts. EOF means cancel, rather than an infinite retry. */
    public static LoginConfig read(String[] args, Scanner input, PrintStream output) {
        String[] candidate = args;
        while (true) {
            try {
                return parse(candidate);
            } catch (IllegalArgumentException e) {
                output.println(e.getMessage());
                output.println("Enter n t, for example 3 10, then press Enter:");
                if (!input.hasNextLine()) {
                    output.println("Input closed. The application was not started.");
                    return null;
                }
                candidate = input.nextLine().trim().split("\\s+");
            }
        }
    }
}
