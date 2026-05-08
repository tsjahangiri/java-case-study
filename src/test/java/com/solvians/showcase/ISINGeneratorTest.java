package com.solvians.showcase;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ISINGenerator}.
 *
 * Groups:
 *   1. Check digit algorithm  — known inputs, hand-calculated expected outputs
 *   2. Generated ISIN format  — structural rules (length, character types)
 *   3. Check digit validity   — generated ISINs pass their own check digit
 *   4. Input validation       — null, wrong length, invalid characters
 *   5. Edge cases             — boundary values and corner cases
 *   6. Regression load tests  — high-volume runs to catch non-deterministic bugs
 */
class ISINGeneratorTest {

    // ── Group 1: Check digit algorithm ───────────────────────────────────────

    @Test
    void computeCheckDigit_readmeExample_returnsCorrectDigit() {
        // README states: "DE123456789" → 6
        assertEquals(6, ISINGenerator.computeCheckDigit("DE123456789"));
    }

    @Test
    void computeCheckDigit_allZeroBody_returnsZero() {
        // sum=0 → (10-0)%10=0
        // Also verifies the outer %10 handles the multiple-of-10 edge case
        assertEquals(0, ISINGenerator.computeCheckDigit("00000000000"));
    }

    @Test
    void computeCheckDigit_lowercaseInput_isTreatedAsUppercase() {
        // "de123456789" should produce same result as "DE123456789"
        assertEquals(
                ISINGenerator.computeCheckDigit("DE123456789"),
                ISINGenerator.computeCheckDigit("de123456789")
        );
    }

    @Test
    void computeCheckDigit_resultIsAlwaysInRange() {
        // Property test — known real-world ISINs
        String[] bodies = {
                "DE123456789",
                "US037833100",
                "GB000263494",
                "JP3633400001".substring(0, 11),
                "ZZ999999999"
        };
        for (String body : bodies) {
            int digit = ISINGenerator.computeCheckDigit(body);
            assertTrue(digit >= 0 && digit <= 9,
                    "Out of range for: " + body + " → " + digit);
        }
    }

    // ── Group 2: Generated ISIN format ───────────────────────────────────────

    @Test
    void generate_producesExactly12Characters() {
        assertEquals(12, ISINGenerator.generate().length());
    }

    @Test
    void generate_firstTwoPositionsAreUppercaseLetters() {
        String isin = ISINGenerator.generate();
        for (int i = 0; i < 2; i++) {
            char c = isin.charAt(i);
            assertTrue(Character.isLetter(c) && Character.isUpperCase(c),
                    "Position " + i + " must be an uppercase letter, got: " + c);
        }
    }

    @Test
    void generate_positions3to11AreUppercaseAlphanumeric() {
        String isin = ISINGenerator.generate();
        for (int i = 2; i <= 10; i++) {
            char c = isin.charAt(i);
            assertTrue(
                    Character.isDigit(c) || (Character.isLetter(c) && Character.isUpperCase(c)),
                    "Position " + i + " must be uppercase alphanumeric, got: " + c
            );
        }
    }

    @Test
    void generate_lastPositionIsDigit() {
        char last = ISINGenerator.generate().charAt(11);
        assertTrue(Character.isDigit(last),
                "Check digit must be a digit, got: " + last);
    }

    // ── Group 3: Check digit validity ────────────────────────────────────────

    @Test
    void generate_checkDigitMatchesComputedValue() {
        String isin            = ISINGenerator.generate();
        int expectedCheckDigit = ISINGenerator.computeCheckDigit(isin.substring(0, 11));
        int actualCheckDigit   = isin.charAt(11) - '0';
        assertEquals(expectedCheckDigit, actualCheckDigit,
                "Check digit mismatch: " + isin);
    }

    @Test
    void generate_deterministicWithSeededRandom() {
        // Same seed → same ISIN — verifies generate(Random) is deterministic
        // Useful to know for reproducing test failures
        Random seeded1 = new Random(42);
        Random seeded2 = new Random(42);
        assertEquals(
                ISINGenerator.generate(seeded1),
                ISINGenerator.generate(seeded2),
                "Same seed must produce same ISIN"
        );
    }

    // ── Group 4: Input validation ─────────────────────────────────────────────

    @Test
    void computeCheckDigit_nullInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                ISINGenerator.computeCheckDigit(null)
        );
    }

    @Test
    void computeCheckDigit_emptyString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                ISINGenerator.computeCheckDigit("")
        );
    }

    @Test
    void computeCheckDigit_tooShort_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ISINGenerator.computeCheckDigit("DE12345")
        );
        assertTrue(ex.getMessage().contains("11"),
                "Error message should mention expected length 11");
    }

    @Test
    void computeCheckDigit_tooLong_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                ISINGenerator.computeCheckDigit("DE1234567890")
        );
    }

    @Test
    void computeCheckDigit_specialCharacters_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                ISINGenerator.computeCheckDigit("DE!23456789")
        );
    }

    @Test
    void computeCheckDigit_spaces_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                ISINGenerator.computeCheckDigit("DE 23456789")
        );
    }

    @Test
    void generate_nullRandom_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                ISINGenerator.generate(null)
        );
    }

    // ── Group 5: Edge cases ───────────────────────────────────────────────────

    @Test
    void computeCheckDigit_allZsAllNines_handlesMaximumValues() {
        // Z=35 — highest letter value, tests upper boundary of formula
        int result = ISINGenerator.computeCheckDigit("ZZ999999999");
        assertTrue(result >= 0 && result <= 9);
    }

    @Test
    void computeCheckDigit_allAs_handlesMinimumLetterValue() {
        // A=10 — lowest letter value, tests lower boundary of formula
        int result = ISINGenerator.computeCheckDigit("AAAAAAAAAAA");
        assertTrue(result >= 0 && result <= 9);
    }

    @Test
    void computeCheckDigit_sumLandsOnMultipleOfTen_returnsZeroNotTen() {
        // Critical edge case: (10 - sum%10) without outer %10 would return 10
        // We need to find or verify a body where sum % 10 == 0
        // "00000000000" produces sum=0 which triggers this path
        int result = ISINGenerator.computeCheckDigit("00000000000");
        assertEquals(0, result, "When sum is multiple of 10, check digit must be 0 not 10");
    }

    // ── Group 6: Regression load tests ───────────────────────────────────────

    @RepeatedTest(100)
    void generate_100Times_allCheckDigitsValid() {
        String isin            = ISINGenerator.generate();
        int expectedCheckDigit = ISINGenerator.computeCheckDigit(isin.substring(0, 11));
        int actualCheckDigit   = isin.charAt(11) - '0';
        assertEquals(expectedCheckDigit, actualCheckDigit,
                "Invalid check digit in: " + isin);
    }

    @Test
    void generate_10000Times_allFormatsValid() {
        // High-volume run — catches non-deterministic bugs that only appear
        // for specific character combinations
        for (int i = 0; i < 10_000; i++) {
            String isin = ISINGenerator.generate();
            assertEquals(12, isin.length(),             "Wrong length at iteration " + i);
            assertTrue(Character.isLetter(isin.charAt(0)), "Non-letter at pos 0, iteration " + i);
            assertTrue(Character.isLetter(isin.charAt(1)), "Non-letter at pos 1, iteration " + i);
            assertTrue(Character.isDigit(isin.charAt(11)), "Non-digit check digit, iteration " + i);
        }
    }

    @Test
    void generate_1000Times_producesReasonableDiversity() {
        // Verifies randomness is working — 1000 ISINs should not all be identical
        // Collision probability with 36^9 body space is astronomically low
        Set<String> unique = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            unique.add(ISINGenerator.generate());
        }
        assertTrue(unique.size() > 990,
                "Expected near-unique ISINs, got only " + unique.size() + " distinct values");
    }
}
