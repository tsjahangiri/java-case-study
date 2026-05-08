package com.solvians.showcase;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates valid ISIN strings.
 *
 * Format: 2 uppercase letters + 9 alphanumeric characters + 1 check digit
 * The check digit is computed using a Luhn-style algorithm as specified.
 */
public class ISINGenerator {

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * Generates a random valid ISIN using the current thread's random.
     */
    public static String generate() {
        return generate(ThreadLocalRandom.current());
    }

    /**
     * Generates a random valid ISIN using the provided random instance.
     */
    public static String generate(ThreadLocalRandom random) {
        StringBuilder body = new StringBuilder(11);

        // 2 random uppercase letters
        for (int i = 0; i < 2; i++) {
            body.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }

        // 9 random alphanumeric characters
        for (int i = 0; i < 9; i++) {
            body.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }

        int checkDigit = computeCheckDigit(body.toString());
        body.append(checkDigit);
        return body.toString();
    }

    /**
     * Computes the ISIN check digit for an 11-character ISIN body (without check digit).
     *
     * Algorithm:
     * 1. Convert each letter to its numeric value: A=10, B=11, ..., Z=35
     * 2. Flatten to a string of individual digits
     * 3. Starting from rightmost digit, double every other digit
     * 4. Sum all digits (two-digit numbers like 14 contribute 1+4)
     * 5. Check digit = (10 - (sum % 10)) % 10
     *
     * Example: "DE123456789" → check digit 6
     */
    public static int computeCheckDigit(String isin11) {
        // Step 1: expand letters to numeric strings
        StringBuilder expanded = new StringBuilder();
        for (char c : isin11.toCharArray()) {
            if (Character.isLetter(c)) {
                expanded.append(c - 'A' + 10); // A=10, B=11, ..., Z=35
            } else {
                expanded.append(c);
            }
        }

        // Step 2: convert to int array of individual digits
        String digitString = expanded.toString();
        int[] digits = new int[digitString.length()];
        for (int i = 0; i < digitString.length(); i++) {
            digits[i] = digitString.charAt(i) - '0';
        }

        // Step 3: from rightmost digit, double every other digit (positions 1, 3, 5, ... from right)
        for (int i = digits.length - 1; i >= 0; i -= 2) {
            digits[i] *= 2;
        }

        // Step 4: sum all digits (splitting two-digit values: 14 → 1+4)
        int sum = 0;
        for (int d : digits) {
            sum += d / 10 + d % 10;
        }

        // Step 5: check digit
        return (10 - (sum % 10)) % 10;
    }
}
