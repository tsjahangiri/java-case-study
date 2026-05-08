package com.solvians.showcase;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates valid ISIN (International Securities Identification Number) strings.
 *
 * <p>An ISIN has exactly 12 characters:
 * <pre>
 *   [ 2 uppercase letters ][ 9 alphanumeric characters ][ 1 check digit ]
 *
 *   e.g.  D  E  1  2  3  4  5  6  7  8  9  6
 *         ^^^^  ^^^^^^^^^^^^^^^^^^^^^^^^^  ^
 *         country code      body         check digit
 * </pre>
 *
 * <p>The check digit is computed using a Luhn-style algorithm that detects
 * common data entry errors such as single digit mistakes and transpositions.
 *
 * <p><b>Design decisions:</b>
 * <ul>
 *   <li>Stateless utility class — all methods are static, no instance needed</li>
 *   <li>{@code generate(Random)} accepts the parent type {@code Random} rather than
 *       {@code ThreadLocalRandom} so callers can pass any random source —
 *       a seeded {@code new Random(42)} for deterministic tests, or
 *       {@code SecureRandom} for cryptographic use cases</li>
 *   <li>Input is normalised to uppercase so callers are not punished for
 *       passing lowercase — defensive but friendly</li>
 * </ul>
 */
public class ISINGenerator {

    /**
     * Character pool for positions 1–2: uppercase letters only (A–Z, 26 chars).
     * These represent the country code portion of the ISIN.
     */
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Character pool for positions 3–11: uppercase letters and digits (A–Z + 0–9, 36 chars).
     * These represent the national securities identifier body.
     */
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * Maximum allowed length for the ISIN body passed to {@link #computeCheckDigit}.
     * An ISIN body is always exactly 11 characters (12 total minus 1 check digit).
     */
    private static final int ISIN_BODY_LENGTH = 11;

    // Private constructor — utility class, not meant to be instantiated
    private ISINGenerator() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates a random valid ISIN using the calling thread's own random instance.
     *
     * <p>Delegates to {@link #generate(Random)} using {@link ThreadLocalRandom#current()}.
     * {@code ThreadLocalRandom} is preferred in multi-threaded contexts because
     * each thread has its own instance — no contention, no lock overhead.
     *
     * @return a valid 12-character ISIN string, never null
     */
    public static String generate() {
        return generate(ThreadLocalRandom.current());
    }

    /**
     * Generates a random valid ISIN using the provided random instance.
     *
     * <p>Accepts {@link Random} (the parent type) rather than {@link ThreadLocalRandom}
     * so callers can control the random source:
     * <ul>
     *   <li>Pass {@code new Random(42)} in tests for deterministic, reproducible output</li>
     *   <li>Pass {@code ThreadLocalRandom.current()} for best multi-threaded performance</li>
     *   <li>Pass {@code new SecureRandom()} if cryptographic randomness is required</li>
     * </ul>
     *
     * @param random the random source to use — must not be null
     * @return a valid 12-character ISIN string, never null
     * @throws IllegalArgumentException if {@code random} is null
     */
    public static String generate(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }

        StringBuilder body = new StringBuilder(ISIN_BODY_LENGTH);

        // Positions 1–2: uppercase letters only (e.g. "DE", "US", "GB")
        for (int i = 0; i < 2; i++) {
            body.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }

        // Positions 3–11: uppercase alphanumeric (letters or digits)
        for (int i = 0; i < 9; i++) {
            body.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }

        // Position 12: computed check digit derived from the 11-char body
        int checkDigit = computeCheckDigit(body.toString());
        body.append(checkDigit);

        return body.toString();
    }

    // ── Check digit algorithm ─────────────────────────────────────────────────

    /**
     * Computes the check digit for an 11-character ISIN body (without check digit).
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Normalise input to uppercase</li>
     *   <li>Convert each letter to its numeric value: A=10, B=11, ..., Z=35</li>
     *   <li>Flatten result to a string of individual digits</li>
     *   <li>Starting from the rightmost digit, double every other digit</li>
     *   <li>Sum all digits — two-digit results (e.g. 14) contribute as 1+4=5</li>
     *   <li>Check digit = (10 − (sum mod 10)) mod 10</li>
     * </ol>
     *
     * <p>Example: {@code "DE123456789"} → check digit {@code 6}
     * <pre>
     *   D=13, E=14 → expanded: "1314123456789"
     *   After doubling alternating digits from right:
     *     [2, 3, 2, 4, 2, 2, 6, 4, 10, 6, 14, 8, 18]
     *   Sum of individual digits:
     *     2+3+2+4+2+2+6+4+1+0+6+1+4+8+1+8 = 54
     *   Check digit: (10 - 54%10) % 10 = (10 - 4) % 10 = 6
     * </pre>
     *
     * @param isin11 the first 11 characters of an ISIN — must not be null,
     *               must be exactly 11 characters, must contain only A–Z and 0–9
     * @return the check digit as an integer in range [0, 9]
     * @throws IllegalArgumentException if input is null, wrong length,
     *                                  or contains invalid characters
     */
    public static int computeCheckDigit(String isin11) {
        // ── Guard: null check ─────────────────────────────────────────────────
        if (isin11 == null) {
            throw new IllegalArgumentException("ISIN body must not be null");
        }

        // ── Guard: normalise to uppercase before any length/content checks ────
        // Defensive — lets callers pass "de123456789" and still get correct result
        isin11 = isin11.toUpperCase();

        // ── Guard: length check ───────────────────────────────────────────────
        if (isin11.length() != ISIN_BODY_LENGTH) {
            throw new IllegalArgumentException(
                    "ISIN body must be exactly " + ISIN_BODY_LENGTH
                            + " characters, got: " + isin11.length()
            );
        }

        // ── Guard: character validity check ───────────────────────────────────
        // Each character must be an uppercase letter (A–Z) or a digit (0–9).
        // Spaces, special characters, or lowercase (post-normalisation) are invalid.
        for (char c : isin11.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                throw new IllegalArgumentException(
                        "ISIN body contains invalid character '" + c
                                + "' — only A–Z and 0–9 are allowed"
                );
            }
        }

        // ── Step 1: expand letters to their numeric string equivalents ────────
        // A=10, B=11, ..., Z=35  (formula: c - 'A' + 10)
        // Digits are appended as-is.
        // Example: "DE123456789" → "1314123456789"
        StringBuilder expanded = new StringBuilder();
        for (char c : isin11.toCharArray()) {
            if (Character.isLetter(c)) {
                expanded.append(c - 'A' + 10);
            } else {
                expanded.append(c);
            }
        }

        // ── Step 2: convert expanded string to int array of individual digits ──
        // '0'-'0'=0, '1'-'0'=1, ..., '9'-'0'=9
        // Example: "1314123456789" → [1,3,1,4,1,2,3,4,5,6,7,8,9]
        String digitString = expanded.toString();
        int[] digits = new int[digitString.length()];
        for (int i = 0; i < digitString.length(); i++) {
            digits[i] = digitString.charAt(i) - '0';
        }

        // ── Step 3: double every other digit starting from the rightmost ──────
        // Positions 1, 3, 5, ... from the right (1-indexed) are doubled.
        // Example: [1,3,1,4,1,2,3,4,5,6,7,8,9]
        //       → [2,3,2,4,2,2,6,4,10,6,14,8,18]
        for (int i = digits.length - 1; i >= 0; i -= 2) {
            digits[i] *= 2;
        }

        // ── Step 4: sum all digits, splitting two-digit numbers ───────────────
        // d/10 extracts tens digit (0 for single-digit numbers)
        // d%10 extracts units digit (the number itself for single digits)
        // Example: 18 → 1+8=9,  14 → 1+4=5,  6 → 0+6=6
        int sum = 0;
        for (int d : digits) {
            sum += d / 10 + d % 10;
        }

        // ── Step 5: compute check digit ───────────────────────────────────────
        // The outer %10 handles the edge case where sum is already a multiple
        // of 10 — without it (10-0) would give 10, not 0.
        // Example: sum=54 → (10-4)%10=6
        // Edge case: sum=60 → (10-0)%10=0
        return (10 - (sum % 10)) % 10;
    }
}
