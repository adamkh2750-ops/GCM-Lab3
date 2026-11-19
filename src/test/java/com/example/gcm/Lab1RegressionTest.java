package com.example.gcm;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class Lab1RegressionTest {
    @Test void originalConsoleProgramStillProducesTheExactExpectedBytes() throws Exception {
        InputStream beforeIn = System.in;
        PrintStream beforeOut = System.out;
        PrintStream beforeErr = System.err;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(Files.readAllBytes(Path.of("users.txt"))));
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(new ByteArrayOutputStream()));
            UsersApp.main(new String[0]);
        } finally {
            System.setIn(beforeIn);
            System.setOut(beforeOut);
            System.setErr(beforeErr);
        }
        assertArrayEquals(Files.readAllBytes(Path.of("expected_out.txt")), output.toByteArray());
    }
}
