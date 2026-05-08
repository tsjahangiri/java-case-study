package com.solvians.showcase;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CertificateUpdateGenerator}.
 *
 * Groups:
 *   1. Count correctness    — output size matches threads × quotes
 *   2. Constructor validation — invalid arguments rejected early
 *   3. Result quality       — all generated items are valid
 *   4. Executor injection   — injected executor behaves correctly
 *   5. Edge cases           — boundary values
 *   6. Regression load      — high-volume runs
 */
class CertificateUpdateGeneratorTest {

    // ── Group 1: Count correctness ────────────────────────────────────────────

    @Test
    void generateQuotes_returnsCorrectCount() {
        // Original test — must always pass
        assertEquals(10 * 100,
                new CertificateUpdateGenerator(10, 100).generateQuotes().count());
    }

    @Test
    void generateQuotes_singleThreadSingleQuote_returnsOne() {
        assertEquals(1,
                new CertificateUpdateGenerator(1, 1).generateQuotes().count());
    }

    @Test
    void generateQuotes_multipleThreadsSingleQuote_returnsThreadCount() {
        assertEquals(5,
                new CertificateUpdateGenerator(5, 1).generateQuotes().count());
    }

    @Test
    void generateQuotes_singleThreadMultipleQuotes_returnsQuoteCount() {
        assertEquals(10,
                new CertificateUpdateGenerator(1, 10).generateQuotes().count());
    }

    // ── Group 2: Constructor validation ──────────────────────────────────────

    @Test
    void constructor_zeroThreads_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new CertificateUpdateGenerator(0, 100)
        );
    }

    @Test
    void constructor_negativeThreads_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new CertificateUpdateGenerator(-1, 100)
        );
    }

    @Test
    void constructor_threadsExceedsMax_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new CertificateUpdateGenerator(
                        CertificateUpdateGenerator.MAX_THREADS + 1, 100)
        );
    }

    @Test
    void constructor_zeroQuotes_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new CertificateUpdateGenerator(10, 0)
        );
    }

    @Test
    void constructor_negativeQuotes_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new CertificateUpdateGenerator(10, -1)
        );
    }

    @Test
    void constructor_quotesExceedMax_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new CertificateUpdateGenerator(
                        10, CertificateUpdateGenerator.MAX_QUOTES + 1)
        );
    }

    @Test
    void constructor_nullExecutor_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                new CertificateUpdateGenerator(10, 100, null)
        );
    }

    // ── Group 3: Result quality ───────────────────────────────────────────────

    @Test
    void generateQuotes_noNullItems() {
        new CertificateUpdateGenerator(4, 10).generateQuotes()
                .forEach(u -> assertNotNull(u, "Stream must not contain nulls"));
    }

    @Test
    void generateQuotes_allItemsProduceValidCsvLines() {
        String pattern = "\\d+,[A-Z0-9]{12},\\d+\\.\\d{2},\\d+,\\d+\\.\\d{2},\\d+";
        new CertificateUpdateGenerator(4, 10).generateQuotes()
                .map(CertificateUpdate::toLine)
                .forEach(line -> assertTrue(line.matches(pattern),
                        "Invalid CSV: " + line));
    }

    @Test
    void generateQuotes_allIsinsAreValid() {
        new CertificateUpdateGenerator(4, 25).generateQuotes()
                .map(CertificateUpdate::getIsin)
                .forEach(isin -> {
                    assertEquals(12, isin.length(), "Wrong ISIN length: " + isin);
                    int expected = ISINGenerator.computeCheckDigit(isin.substring(0, 11));
                    assertEquals(expected, isin.charAt(11) - '0',
                            "Invalid check digit: " + isin);
                });
    }

    @Test
    void generateQuotes_allPricesWithinRange() {
        new CertificateUpdateGenerator(4, 25).generateQuotes()
                .forEach(u -> {
                    assertTrue(u.getBidPrice() >= 100.0 && u.getBidPrice() <= 200.0);
                    assertTrue(u.getAskPrice() >= 100.0 && u.getAskPrice() <= 200.0);
                });
    }

    @Test
    void generateQuotes_allSizesWithinRange() {
        new CertificateUpdateGenerator(4, 25).generateQuotes()
                .forEach(u -> {
                    assertTrue(u.getBidSize() >= 1000  && u.getBidSize() <= 5000);
                    assertTrue(u.getAskSize() >= 1000  && u.getAskSize() <= 10000);
                });
    }

    // ── Group 4: Executor injection ───────────────────────────────────────────

    @Test
    void generateQuotes_withSingleThreadedExecutor_stillProducesCorrectCount() {
        CertificateUpdateGenerator gen = new CertificateUpdateGenerator(
                5, 10, Executors.newSingleThreadExecutor()
        );
        assertEquals(50, gen.generateQuotes().count());
    }

    @Test
    void generateQuotes_withCachedThreadPool_producesCorrectCount() {
        CertificateUpdateGenerator gen = new CertificateUpdateGenerator(
                5, 10, Executors.newCachedThreadPool()
        );
        assertEquals(50, gen.generateQuotes().count());
    }

    // ── Group 5: Edge cases ───────────────────────────────────────────────────

    @Test
    void generateQuotes_minimumArgs_producesOneItem() {
        assertEquals(1,
                new CertificateUpdateGenerator(1, 1).generateQuotes().count());
    }

    @Test
    void generateQuotes_maxBoundaryThreads_doesNotThrow() {
        assertDoesNotThrow(() ->
                new CertificateUpdateGenerator(
                        CertificateUpdateGenerator.MAX_THREADS, 1
                ).generateQuotes().count()
        );
    }

    @Test
    void generateQuotes_producesDistinctIsins() {
        // Not a strict guarantee but collision in 500 items is astronomically unlikely
        List<String> isins = new CertificateUpdateGenerator(5, 100)
                .generateQuotes()
                .map(CertificateUpdate::getIsin)
                .collect(Collectors.toList());

        Set<String> unique = new HashSet<>(isins);
        assertTrue(unique.size() > isins.size() * 0.99,
                "Expected near-unique ISINs, collisions: " + (isins.size() - unique.size()));
    }

    // ── Group 6: Regression load tests ───────────────────────────────────────

    @Test
    void generateQuotes_largeVolume_allItemsValid() {
        // 10 threads × 1000 quotes = 10,000 items
        // Catches race conditions and thread-safety issues that only appear under load
        String pattern = "\\d+,[A-Z0-9]{12},\\d+\\.\\d{2},\\d+,\\d+\\.\\d{2},\\d+";
        long validCount = new CertificateUpdateGenerator(10, 1000)
                .generateQuotes()
                .map(CertificateUpdate::toLine)
                .filter(line -> line.matches(pattern))
                .count();
        assertEquals(10_000, validCount,
                "All 10,000 lines must be valid");
    }

    @Test
    void generateQuotes_repeatedCalls_produceDifferentResults() {
        // Running the generator twice should not produce identical output
        // Catches bugs where random state is accidentally shared or reset
        CertificateUpdateGenerator gen = new CertificateUpdateGenerator(2, 5);

        Set<String> firstRun = gen.generateQuotes()
                .map(CertificateUpdate::getIsin)
                .collect(Collectors.toSet());

        Set<String> secondRun = gen.generateQuotes()
                .map(CertificateUpdate::getIsin)
                .collect(Collectors.toSet());

        // The two runs should not be completely identical
        assertNotEquals(firstRun, secondRun,
                "Two runs should not produce exactly the same ISINs");
    }
}
