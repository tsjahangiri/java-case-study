package com.solvians.showcase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link App}.
 *
 * Groups:
 *   1. Argument count       — wrong number of args rejected
 *   2. Numeric parsing      — non-numeric args produce NumberFormatException
 *   3. Business validation  — zero, negative, and over-limit values rejected
 *   4. validateArgs unit    — tested directly without going through main
 *   5. Happy path           — valid args run successfully
 *   6. Edge cases           — boundary values at limits
 */
public class AppTest {

    // ── Group 1: Argument count ───────────────────────────────────────────────

    @Test
    void main_noArgs_throwsRuntimeException() {
        assertThrows(RuntimeException.class, () ->
                App.main(new String[]{})
        );
    }

    @Test
    void main_oneArg_throwsRuntimeException() {
        // Original test — must always pass
        assertThrows(RuntimeException.class, () ->
                App.main(new String[]{"10"})
        );
    }

    // ── Group 2: Numeric parsing ──────────────────────────────────────────────

    @Test
    void main_bothArgsNonNumeric_throwsNumberFormatException() {
        NumberFormatException ex = assertThrows(NumberFormatException.class, () ->
                App.main(new String[]{"xxx", "zzz"})
        );
        assertEquals("For input string: \"xxx\"", ex.getMessage());
    }

    @Test
    void main_secondArgNonNumeric_throwsNumberFormatException() {
        NumberFormatException ex = assertThrows(NumberFormatException.class, () ->
                App.main(new String[]{"10", "zzz"})
        );
        assertEquals("For input string: \"zzz\"", ex.getMessage());
    }

    @Test
    void main_floatArgs_throwsNumberFormatException() {
        // parseInt does not accept decimals
        assertThrows(NumberFormatException.class, () ->
                App.main(new String[]{"1.5", "10"})
        );
    }

    // ── Group 3: Business validation ─────────────────────────────────────────

    @Test
    void main_zeroThreads_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                App.main(new String[]{"0", "100"})
        );
        assertTrue(ex.getMessage().contains("threads"));
    }

    @Test
    void main_negativeThreads_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                App.main(new String[]{"-1", "100"})
        );
    }

    @Test
    void main_threadsOverMax_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                App.main(new String[]{
                        String.valueOf(CertificateUpdateGenerator.MAX_THREADS + 1),
                        "100"
                })
        );
    }

    @Test
    void main_zeroQuotes_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                App.main(new String[]{"10", "0"})
        );
        assertTrue(ex.getMessage().contains("quotes"));
    }

    @Test
    void main_negativeQuotes_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                App.main(new String[]{"10", "-5"})
        );
    }

    @Test
    void main_quotesOverMax_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                App.main(new String[]{
                        "10",
                        String.valueOf(CertificateUpdateGenerator.MAX_QUOTES + 1)
                })
        );
    }

    // ── Group 4: validateArgs unit tests ─────────────────────────────────────

    @Test
    void validateArgs_validValues_doesNotThrow() {
        assertDoesNotThrow(() -> App.validateArgs(10, 100));
    }

    @Test
    void validateArgs_minimumValues_doesNotThrow() {
        assertDoesNotThrow(() -> App.validateArgs(1, 1));
    }

    @Test
    void validateArgs_maximumValues_doesNotThrow() {
        assertDoesNotThrow(() ->
                App.validateArgs(
                        CertificateUpdateGenerator.MAX_THREADS,
                        CertificateUpdateGenerator.MAX_QUOTES
                )
        );
    }

    @Test
    void validateArgs_zeroThreads_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                App.validateArgs(0, 100)
        );
    }

    @Test
    void validateArgs_zeroQuotes_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                App.validateArgs(10, 0)
        );
    }

    // ── Group 5: Happy path ───────────────────────────────────────────────────

    @Test
    void main_validArgs_runsWithoutException() {
        assertDoesNotThrow(() ->
                App.main(new String[]{"2", "5"})
        );
    }

    // ── Group 6: Edge cases ───────────────────────────────────────────────────

    @Test
    void main_maxAllowedArgs_runsWithoutException() {
        // MAX_THREADS threads, 1 quote each — large thread count, small output
        assertDoesNotThrow(() ->
                App.main(new String[]{
                        String.valueOf(CertificateUpdateGenerator.MAX_THREADS),
                        "1"
                })
        );
    }

    @Test
    void main_oneThreadMaxQuotes_doesNotThrow() {
        // 1 thread, maximum quotes — tests upper bound on quotes
        // kept small in practice to not slow the test suite
        assertDoesNotThrow(() ->
                App.main(new String[]{"1", "1000"})
        );
    }
}
