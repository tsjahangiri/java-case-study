package com.solvians.showcase;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents one certificate update with all required market data fields.
 *
 * <p>Implements {@link Callable}{@code <String>} so instances can be submitted
 * directly to an {@link java.util.concurrent.ExecutorService}.
 *
 * <p>All field values are generated in the constructor so the object is
 * fully immutable by the time it leaves the constructor.
 * Immutability means this object is safe to read from multiple threads
 * simultaneously without synchronisation.
 *
 * <p>Field ranges:
 * <ul>
 *   <li>Timestamp  — current epoch milliseconds, always positive</li>
 *   <li>ISIN       — valid 12-character ISIN (see {@link ISINGenerator})</li>
 *   <li>Bid price  — [100.00, 200.00], 2 decimal places</li>
 *   <li>Bid size   — [1 000, 5 000]</li>
 *   <li>Ask price  — [100.00, 200.00], 2 decimal places</li>
 *   <li>Ask size   — [1 000, 10 000]</li>
 * </ul>
 */
public class CertificateUpdate implements Callable<String> {

    private final long   timestamp;
    private final String isin;
    private final double bidPrice;
    private final int    bidSize;
    private final double askPrice;
    private final int    askSize;

    /**
     * Creates a {@code CertificateUpdate} with randomly generated values.
     *
     * <p>Uses {@link ThreadLocalRandom} — each thread has its own instance
     * so there is no contention between threads constructing updates simultaneously.
     *
     * <p>Calls {@link #validate()} after generating all fields to catch any
     * range violations early — fail fast rather than producing corrupt CSV output.
     *
     * @throws IllegalStateException if any generated field is outside its valid range
     */
    public CertificateUpdate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        this.timestamp = System.currentTimeMillis();
        this.isin      = ISINGenerator.generate(random);

        // Prices: integer cents in [0, 10000] + 10000 → [10000, 20000]
        // Dividing by 100.0 gives range [100.00, 200.00]
        this.bidPrice  = random.nextInt(10001) / 100.0 + 100.0;
        this.askPrice  = random.nextInt(10001) / 100.0 + 100.0;

        // Sizes: nextInt(n) gives [0, n-1], so +1000 shifts range to desired minimum
        this.bidSize   = random.nextInt(4001) + 1000;   // [1000, 5000]
        this.askSize   = random.nextInt(9001) + 1000;   // [1000, 10000]

        // Validate immediately after construction — fail fast if any field is wrong
        validate();
    }

    // ── Callable contract ─────────────────────────────────────────────────────

    /**
     * Returns the comma-separated CSV line for this certificate update.
     *
     * <p>Format: {@code timestamp,ISIN,bidPrice,bidSize,askPrice,askSize}
     * <br>Example: {@code 1352122280502,DE1234567896,101.23,1000,103.45,2000}
     *
     * <p>Prices are formatted to exactly 2 decimal places using {@code %.2f}.
     * Sizes are formatted as plain integers using {@code %d}.
     *
     * @return the CSV line — never null, never empty
     * @throws Exception declared by the {@link Callable} interface —
     *                   in practice this implementation never throws
     */
    @Override
    public String call() throws Exception {
        return String.format("%d,%s,%.2f,%d,%.2f,%d",
                timestamp, isin, bidPrice, bidSize, askPrice, askSize);
    }

    /**
     * Returns the CSV line without throwing a checked exception.
     *
     * <p>Prefer this over {@link #call()} in application code to avoid
     * wrapping in try/catch at every call site.
     * {@code App} and stream pipelines use this method.
     *
     * @return the CSV line — never null, never empty
     * @throws RuntimeException if formatting fails (should never happen in practice)
     */
    public String toLine() {
        try {
            return call();
        } catch (Exception e) {
            throw new RuntimeException("Failed to format certificate update", e);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public long   getTimestamp() { return timestamp; }
    public String getIsin()      { return isin; }
    public double getBidPrice()  { return bidPrice; }
    public int    getBidSize()   { return bidSize; }
    public double getAskPrice()  { return askPrice; }
    public int    getAskSize()   { return askSize; }

    // ── Object overrides ──────────────────────────────────────────────────────

    /**
     * Returns the CSV line — consistent with {@link #toLine()} and {@link #call()}.
     * Useful when logging or printing a {@code CertificateUpdate} during debugging.
     */
    @Override
    public String toString() {
        return toLine();
    }

    /**
     * Two {@code CertificateUpdate} instances are equal if all six fields match.
     *
     * <p>Implemented because updates may end up in collections ({@code Set}, {@code Map})
     * and Java's default reference equality would treat two identical updates as different.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CertificateUpdate)) return false;
        CertificateUpdate that = (CertificateUpdate) o;
        return timestamp == that.timestamp
                && bidSize   == that.bidSize
                && askSize   == that.askSize
                && Double.compare(bidPrice, that.bidPrice) == 0
                && Double.compare(askPrice, that.askPrice) == 0
                && isin.equals(that.isin);
    }

    /**
     * Consistent with {@link #equals} — objects that are equal have the same hash code.
     */
    @Override
    public int hashCode() {
        int result = Long.hashCode(timestamp);
        result = 31 * result + isin.hashCode();
        result = 31 * result + Double.hashCode(bidPrice);
        result = 31 * result + Integer.hashCode(bidSize);
        result = 31 * result + Double.hashCode(askPrice);
        result = 31 * result + Integer.hashCode(askSize);
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Validates all generated fields are within their specified ranges.
     *
     * <p>Called immediately after construction — fail fast so corrupt data
     * never reaches the CSV output or downstream consumers.
     *
     * @throws IllegalStateException if any field is outside its valid range
     */
    private void validate() {
        if (isin == null || isin.length() != 12) {
            throw new IllegalStateException("Generated invalid ISIN: " + isin);
        }
        if (bidPrice < 100.0 || bidPrice > 200.0) {
            throw new IllegalStateException("Bid price out of range: " + bidPrice);
        }
        if (bidSize < 1000 || bidSize > 5000) {
            throw new IllegalStateException("Bid size out of range: " + bidSize);
        }
        if (askPrice < 100.0 || askPrice > 200.0) {
            throw new IllegalStateException("Ask price out of range: " + askPrice);
        }
        if (askSize < 1000 || askSize > 10000) {
            throw new IllegalStateException("Ask size out of range: " + askSize);
        }
    }
}
