package com.example.gcm;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import static org.junit.jupiter.api.Assertions.*;

class LoginConfigTest {
    @Test void acceptsPositiveArgumentsWithoutReadingInput() {
        assertEquals(new LoginConfig(3, 10), LoginConfig.parse(new String[]{"3", "10"}));
        assertEquals(new LoginConfig(1, 1), LoginConfig.parse(new String[]{"1", "1"}));
    }

    @Test void rejectsMissingExtraNonIntegerNonPositiveAndOverflowValues() {
        String[][] invalid = {{}, {"3"}, {"1", "2", "3"}, {"x", "2"}, {"2", "1.5"},
                {"0", "2"}, {"2", "0"}, {"-1", "2"}, {"2", "-1"}, {"2147483648", "2"}};
        for (String[] args : invalid) {
            assertThrows(IllegalArgumentException.class, () -> LoginConfig.parse(args));
        }
    }

    @Test void repeatsManualPromptUntilBothValuesAreValid() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        LoginConfig config = LoginConfig.read(new String[]{"bad"},
                new Scanner("0 4\n3 nope\n3 10\n"), new PrintStream(output));
        assertEquals(new LoginConfig(3, 10), config);
        assertEquals(3, output.toString().split("Enter n t", -1).length - 1);
    }

    @Test void endOfInputCancelsWithoutLaunchingOrSpinning() {
        assertNull(LoginConfig.read(new String[0], new Scanner(""),
                new PrintStream(new ByteArrayOutputStream())));
    }
}
