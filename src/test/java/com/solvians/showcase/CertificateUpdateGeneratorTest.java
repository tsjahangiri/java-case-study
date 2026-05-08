package com.solvians.showcase;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CertificateUpdateGeneratorTest {

    // ── Original test — must still pass ──────────────────────────────────────

    @Test
    void generateQuotes_returnsCorrectCount() {
        CertificateUpdateGenerator generator =
                new CertificateUpdateGenerator(10, 100);
        Stream<CertificateUpdate> quotes = generator.generateQuotes();
        assertNotNull(quotes);
        assertEquals(10 * 100, quotes.count());
    }

    // ── Count variations ──────────────────────────────────────────────────────

    @Test
    void generateQuotes_returnsCorrectCount_forSmallInput() {
        CertificateUpdateGenerator generator =
                new CertificateUpdateGenerator(2, 3);
        assertEquals(6, generator.generateQuotes().count());
    }

    @Test
    void generateQuotes_returnsSingleItem_whenThreadsAndQuotesAreOne() {
        CertificateUpdateGenerator generator =
                new CertificateUpdateGenerator(1, 1);
        assertEquals(1, generator.generateQuotes().count());
    }

    // ── Result quality ────────────────────────────────────────────────────────

    @Test
    void generateQuotes_allItemsAreNonNull() {
        CertificateUpdateGenerator generator =
                new CertificateUpdateGenerator(4, 10);
        generator.generateQuotes()
                .forEach(update -> assertNotNull(update,
                        "Stream should not contain null updates"));
    }

    @Test
    void generateQuotes_allItemsProduceValidCsvLines() {
        CertificateUpdateGenerator generator =
                new CertificateUpdateGenerator(4, 10);
        generator.generateQuotes()
                .map(CertificateUpdate::toLine)
                .forEach(line ->
                        assertTrue(
                                line.matches("\\d+,[A-Z0-9]{12},\\d+\\.\\d{2},\\d+,\\d+\\.\\d{2},\\d+"),
                                "Invalid CSV line: " + line
                        )
                );
    }

    @Test
    void generateQuotes_allIsinsAreValid() {
        CertificateUpdateGenerator generator =
                new CertificateUpdateGenerator(4, 25);

        generator.generateQuotes()
                .map(CertificateUpdate::getIsin)
                .forEach(isin -> {
                    assertEquals(12, isin.length(),
                            "ISIN must be 12 chars: " + isin);
                    int expectedCheck =
                            ISINGenerator.computeCheckDigit(isin.substring(0, 11));
                    assertEquals(expectedCheck, isin.charAt(11) - '0',
                            "Invalid check digit in: " + isin);
                });
    }

    // ── Executor injection ────────────────────────────────────────────────────

    @Test
    void generateQuotes_worksWithInjectedSingleThreadedExecutor() {
        // Single-threaded executor — predictable, no race conditions in tests
        CertificateUpdateGenerator generator = new CertificateUpdateGenerator(
                5, 10,
                Executors.newSingleThreadExecutor()
        );
        assertEquals(50, generator.generateQuotes().count());
    }

    @Test
    void generateQuotes_producesUniqueTimestamps_mostOfTheTime() {
        // Not all timestamps will differ (same millisecond) but the stream
        // should not be all identical — this catches obvious bugs like
        // a static or hardcoded timestamp
        CertificateUpdateGenerator generator =
                new CertificateUpdateGenerator(4, 25);

        List<Long> timestamps = generator.generateQuotes()
                .map(CertificateUpdate::getTimestamp)
                .distinct()
                .collect(Collectors.toList());

        assertTrue(timestamps.size() > 1,
                "Expected multiple distinct timestamps across 100 updates");
    }
}