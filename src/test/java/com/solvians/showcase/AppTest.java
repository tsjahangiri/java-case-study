package com.solvians.showcase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

    // ── Argument count validation ─────────────────────────────────────────────

    @Test
    void main_throwsRuntimeException_whenNoArgsProvided() {
        assertThrows(RuntimeException.class, () ->
                App.main(new String[]{})
        );
    }

    @Test
    void main_throwsRuntimeException_whenOnlyOneArgProvided() {
        // Original test kept — ensures single arg still throws RuntimeException
        assertThrows(RuntimeException.class, () ->
                App.main(new String[]{"10"})
        );
    }

    // ── Numeric parsing validation ────────────────────────────────────────────

    @Test
    void main_throwsNumberFormatException_whenBothArgsNonNumeric() {
        NumberFormatException ex = assertThrows(NumberFormatException.class, () ->
                App.main(new String[]{"xxx", "zzz"})
        );
        assertEquals("For input string: \"xxx\"", ex.getMessage());
    }

    @Test
    void main_throwsNumberFormatException_whenSecondArgNonNumeric() {
        NumberFormatException ex = assertThrows(NumberFormatException.class, () ->
                App.main(new String[]{"10", "zzz"})
        );
        assertEquals("For input string: \"zzz\"", ex.getMessage());
    }

    // ── Business rule validation ──────────────────────────────────────────────

    @Test
    void main_throwsIllegalArgumentException_whenThreadsIsZero() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                App.main(new String[]{"0", "100"})
        );
        assertTrue(ex.getMessage().contains("threads"));
    }

    @Test
    void main_throwsIllegalArgumentException_whenThreadsIsNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                App.main(new String[]{"-1", "100"})
        );
    }

    @Test
    void main_throwsIllegalArgumentException_whenQuotesIsZero() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                App.main(new String[]{"10", "0"})
        );
        assertTrue(ex.getMessage().contains("quotes"));
    }

    @Test
    void main_throwsIllegalArgumentException_whenQuotesIsNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                App.main(new String[]{"10", "-5"})
        );
    }

    // ── validateArgs unit tests (tested independently of main) ───────────────

    @Test
    void validateArgs_doesNotThrow_whenBothValuesArePositive() {
        assertDoesNotThrow(() -> App.validateArgs(10, 100));
    }

    @Test
    void validateArgs_throwsIllegalArgumentException_whenThreadsIsZero() {
        assertThrows(IllegalArgumentException.class, () ->
                App.validateArgs(0, 100)
        );
    }

    @Test
    void validateArgs_throwsIllegalArgumentException_whenQuotesIsZero() {
        assertThrows(IllegalArgumentException.class, () ->
                App.validateArgs(10, 0)
        );
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void main_runsSuccessfully_withValidArgs() {
        // Should complete without throwing any exception
        assertDoesNotThrow(() ->
                App.main(new String[]{"2", "5"})
        );
    }
}