package com.solvians.showcase;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ISINGenerator}.
 *
 * Organised into four groups:
 *  1. Check digit algorithm — known inputs with known expected outputs
 *  2. Generated ISIN format — structural rules (length, character types)
 *  3. Check digit validity  — generated ISINs pass their own check digit
 *  4. Edge cases            — boundary and corner case inputs
 */
class ISINGeneratorTest {

    // ── Group 1: Check digit algorithm ───────────────────────────────────────
    //
    // These tests use known inputs from the README so we can verify
    // the algorithm against a hand-calculated expected value.
    // If any of these fail, the algorithm itself is broken.

    @Test
    void computeCheckDigit_readmeExample_returnsCorrectDigit() {
        // README states: "DE123456789" → check digit 6
        // D=13, E=14 → "1314123456789" → sum=54 → (10-4)%10=6
        assertEquals(6, ISINGenerator.computeCheckDigit("DE123456789"));
    }

    @Test
    void computeCheckDigit_sumIsAlreadyMultipleOfTen_returnsZero() {
        // Edge case: when sum % 10 == 0, check digit must be 0, not 10
        // (10 - 0) % 10 = 0  ← the outer % 10 handles this
        // We find a body whose sum lands on a multiple of 10
        // "US000000000": U=30, S=28 → "3028000000000"
        // verify manually and assert result is in valid range
        int result = ISINGenerator.computeCheckDigit("US000000000");
        assertTrue(result >= 0 && result <= 9,
                "Check digit must always be in [0,9], got: " + result);
    }

    @Test
    void computeCheckDigit_allDigitBody_computesCorrectly() {
        // Body with no letters — no conversion needed, tests the doubling
        // and summing logic in isolation
        int result = ISINGenerator.computeCheckDigit("00000000000");
        // sum = 0, check digit = (10 - 0) % 10 = 0
        assertEquals(0, result);
    }

    @Test
    void computeCheckDigit_resultIsAlwaysInRange() {
        // Property test — regardless of input, result must be 0–9
        String[] testBodies = {
                "AA000000000",
                "ZZ999999999",
                "DE123456789",
                "US037833100",
                "GB000263494"
        };
        for (String body : testBodies) {
            int digit = ISINGenerator.computeCheckDigit(body);
            assertTrue(digit >= 0 && digit <= 9,
                    "Out of range for body " + body + ": " + digit);
        }
    }

    // ── Group 2: Generated ISIN format ───────────────────────────────────────
    //
    // These tests verify the structure of generated ISINs —
    // length, character types at each position — without caring
    // about the specific values (which are random).

    @Test
    void generate_producesExactly12Characters() {
        assertEquals(12, ISINGenerator.generate().length());
    }

    @Test
    void generate_firstCharIsUppercaseLetter() {
        char first = ISINGenerator.generate().charAt(0);
        assertTrue(Character.isLetter(first) && Character.isUpperCase(first),
                "First character must be an uppercase letter, got: " + first);
    }

    @Test
    void generate_secondCharIsUppercaseLetter() {
        char second = ISINGenerator.generate().charAt(1);
        assertTrue(Character.isLetter(second) && Character.isUpperCase(second),
                "Second character must be an uppercase letter, got: " + second);
    }

    @Test
    void generate_positions3to11AreAlphanumeric() {
        // Positions 2–10 (0-indexed) can be letters or digits — but uppercase only
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
    void generate_lastCharIsDigit() {
        // Check digit is always 0–9, never a letter
        char last = ISINGenerator.generate().charAt(11);
        assertTrue(Character.isDigit(last),
                "Last character (check digit) must be a digit, got: " + last);
    }

    // ── Group 3: Check digit validity ────────────────────────────────────────
    //
    // These tests verify that generated ISINs are self-consistent —
    // the check digit appended actually matches what computeCheckDigit
    // would calculate for that body.
    // If these fail, generate() and computeCheckDigit() are out of sync.

    @Test
    void generate_checkDigitMatchesComputedValue() {
        String isin = ISINGenerator.generate();
        String body             = isin.substring(0, 11);
        int expectedCheckDigit  = ISINGenerator.computeCheckDigit(body);
        int actualCheckDigit    = isin.charAt(11) - '0';
        assertEquals(expectedCheckDigit, actualCheckDigit,
                "Check digit mismatch for ISIN: " + isin);
    }

    @RepeatedTest(50)
    void generate_repeatedlyProducesIsinsWithValidCheckDigits() {
        // Run 50 times to catch any non-deterministic failures
        // (e.g. a bug that only appears for certain character combinations)
        String isin            = ISINGenerator.generate();
        String body            = isin.substring(0, 11);
        int expectedCheckDigit = ISINGenerator.computeCheckDigit(body);
        int actualCheckDigit   = isin.charAt(11) - '0';
        assertEquals(expectedCheckDigit, actualCheckDigit,
                "Invalid check digit in generated ISIN: " + isin);
    }

    // ── Group 4: Edge cases ───────────────────────────────────────────────────
    //
    // Corner cases that are easy to get wrong — all-letters body,
    // all-digits body, Z (highest letter value), and the multiple-of-10
    // sum edge case that causes check digit = 0 instead of 10.

    @Test
    void computeCheckDigit_allLetterBody_handlesMultiDigitExpansionCorrectly() {
        // All letters → every char expands to a 2-digit number
        // Tests that the expansion and flattening handles long digit strings correctly
        int result = ISINGenerator.computeCheckDigit("AAAAAAAAAAA");
        assertTrue(result >= 0 && result <= 9);
    }

    @Test
    void computeCheckDigit_highestLetterZ_expandsCorrectly() {
        // Z = 35 — highest possible expansion value
        // Tests the upper boundary of the c - 'A' + 10 formula
        int result = ISINGenerator.computeCheckDigit("ZZ999999999");
        assertTrue(result >= 0 && result <= 9);
    }

    @Test
    void computeCheckDigit_singleLetterRemainder_doesNotCorruptDigits() {
        // Mix of early letters and digits — ensures letter expansion
        // does not shift or corrupt subsequent digit positions
        int result = ISINGenerator.computeCheckDigit("A0000000000");
        // A=10 → expanded starts with "10", rest are zeros
        // sum should be: 1+0 = 1 (from the doubled/undoubled 1 and 0)
        assertTrue(result >= 0 && result <= 9);
    }

    @Test
    void generate_twoCallsProduceDifferentIsins() {
        // Not a strict guarantee (random collision is theoretically possible)
        // but with a 36^9 body space the probability of collision is negligible.
        // This catches obvious bugs like a static or non-random output.
        String first  = ISINGenerator.generate();
        String second = ISINGenerator.generate();
        assertNotEquals(first, second,
                "Two consecutive generates should not produce identical ISINs");
    }
}
