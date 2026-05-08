package com.solvians.showcase;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class AppTest {
    @Test
    public void expectTwoIntArgs() {
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            App.main(new String[]{"xxx"});
        });
        NumberFormatException numbers = Assertions.assertThrows(NumberFormatException.class, () -> {
            App.main(new String[]{"xxx", "zzz"});
        });
        numbers = Assertions.assertThrows(NumberFormatException.class, () -> {
            App.main(new String[]{"10", "zzz"});
        });
        assertEquals("For input string: \"zzz\"", numbers.getMessage());
    }


}
