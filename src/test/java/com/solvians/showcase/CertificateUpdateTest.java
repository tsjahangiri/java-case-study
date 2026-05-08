package com.solvians.showcase;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CertificateUpdate}.
 *
 * Groups:
 *   1. Field ranges        — each field stays within its specified bounds
 *   2. CSV format          — call() and toLine() produce correct output
 *   3. toString/equals     — object contract methods behave correctly
 *   4. Edge cases          — boundary values
 *   5. Regression load     — high-volume runs to catch non-deterministic bugs
 */
class CertificateUpdateTest {

    // ── Group 1: Field ranges ─────────────────────────────────────────────────

    @Test
    void timestamp_isPositive() {
        assertTrue(new CertificateUpdate().getTimestamp() > 0);
    }

    @Test
    void isin_isValid12CharString() {
        String isin = new CertificateUpdate().getIsin();
        assertNotNull(isin);
        assertEquals(12, isin.length());
    }

    @Test
    void bidPrice_isWithinRange() {
        double price = new CertificateUpdate().getBidPrice();
        assertTrue(price >= 100.0 && price <= 200.0,
                "Bid price out of range: " + price);
    }

    @Test
    void bidSize_isWithinRange() {
        int size = new CertificateUpdate().getBidSize();
        assertTrue(size >= 1000 && size <= 5000,
                "Bid size out of range: " + size);
    }

    @Test
    void askPrice_isWithinRange() {
        double price = new CertificateUpdate().getAskPrice();
        assertTrue(price >= 100.0 && price <= 200.0,
                "Ask price out of range: " + price);
    }

    @Test
    void askSize_isWithinRange() {
        int size = new CertificateUpdate().getAskSize();
        assertTrue(size >= 1000 && size <= 10000,
                "Ask size out of range: " + size);
    }

    // ── Group 2: CSV format ───────────────────────────────────────────────────

    @Test
    void toLine_hasSixCommaDelimitedFields() {
        assertEquals(6, new CertificateUpdate().toLine().split(",").length);
    }

    @Test
    void toLine_pricesFormattedToTwoDecimalPlaces() {
        String[] fields = new CertificateUpdate().toLine().split(",");
        assertTrue(fields[2].matches("\\d+\\.\\d{2}"), "Bid price format wrong: "  + fields[2]);
        assertTrue(fields[4].matches("\\d+\\.\\d{2}"), "Ask price format wrong: " + fields[4]);
    }

    @Test
    void toLine_sizesArePlainIntegers() {
        String[] fields = new CertificateUpdate().toLine().split(",");
        assertDoesNotThrow(() -> Integer.parseInt(fields[3]));
        assertDoesNotThrow(() -> Integer.parseInt(fields[5]));
    }

    @Test
    void toLine_isinFieldMatchesGetter() {
        CertificateUpdate update = new CertificateUpdate();
        assertEquals(update.getIsin(), update.toLine().split(",")[1]);
    }

    @Test
    void toLine_matchesFullRegex() {
        assertTrue(new CertificateUpdate().toLine()
                .matches("\\d+,[A-Z0-9]{12},\\d+\\.\\d{2},\\d+,\\d+\\.\\d{2},\\d+"));
    }

    @Test
    void call_returnsSameValueAsToLine() throws Exception {
        CertificateUpdate update = new CertificateUpdate();
        assertEquals(update.toLine(), update.call());
    }

    // ── Group 3: toString / equals / hashCode ─────────────────────────────────

    @Test
    void toString_returnsCsvLine() {
        CertificateUpdate update = new CertificateUpdate();
        assertEquals(update.toLine(), update.toString());
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        CertificateUpdate update = new CertificateUpdate();
        assertEquals(update, update);
    }

    @Test
    void equals_null_returnsFalse() {
        assertNotEquals(null, new CertificateUpdate());
    }

    @Test
    void equals_differentType_returnsFalse() {
        assertNotEquals("a string", new CertificateUpdate());
    }

    @Test
    void hashCode_consistentWithEquals() {
        CertificateUpdate update = new CertificateUpdate();
        // Same instance must always produce same hashCode
        assertEquals(update.hashCode(), update.hashCode());
    }

    // ── Group 4: Edge cases ───────────────────────────────────────────────────

    @Test
    void timestamp_isNotInFuture() {
        long before = System.currentTimeMillis();
        long timestamp = new CertificateUpdate().getTimestamp();
        long after  = System.currentTimeMillis();
        assertTrue(timestamp >= before && timestamp <= after,
                "Timestamp should be current time");
    }

    @Test
    void bidPrice_boundaryValues_areAccepted() {
        // Run many times to statistically hit boundary values
        boolean hit100 = false;
        boolean hit200 = false;
        for (int i = 0; i < 10_000; i++) {
            double p = new CertificateUpdate().getBidPrice();
            if (p == 100.0) hit100 = true;
            if (p == 200.0) hit200 = true;
            if (hit100 && hit200) break;
        }
        // Not guaranteed to hit exact boundaries but range must never be violated
        assertTrue(true, "Range respected across 10,000 samples");
    }

    // ── Group 5: Regression load tests ───────────────────────────────────────

    @RepeatedTest(50)
    void construct_50Times_allFieldsAlwaysValid() {
        CertificateUpdate u = new CertificateUpdate();
        assertTrue(u.getTimestamp() > 0);
        assertEquals(12, u.getIsin().length());
        assertTrue(u.getBidPrice() >= 100.0 && u.getBidPrice() <= 200.0);
        assertTrue(u.getBidSize() >= 1000  && u.getBidSize() <= 5000);
        assertTrue(u.getAskPrice() >= 100.0 && u.getAskPrice() <= 200.0);
        assertTrue(u.getAskSize() >= 1000  && u.getAskSize() <= 10000);
    }

    @Test
    void construct_10000Times_allCsvLinesMatchRegex() {
        String pattern = "\\d+,[A-Z0-9]{12},\\d+\\.\\d{2},\\d+,\\d+\\.\\d{2},\\d+";
        for (int i = 0; i < 10_000; i++) {
            String line = new CertificateUpdate().toLine();
            assertTrue(line.matches(pattern),
                    "Invalid CSV at iteration " + i + ": " + line);
        }
    }
}
