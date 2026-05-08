package com.solvians.showcase;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents one certificate update with all required market data fields.
 *
 * Implements {@link Callable}{@code <String>} so it can be submitted to an
 * {@link java.util.concurrent.ExecutorService} directly.  Calling {@link #call()}
 * returns one comma-separated line in the format:
 *
 * <pre>timestamp,ISIN,bidPrice,bidSize,askPrice,askSize</pre>
 *
 * Example: {@code 1352122280502,DE1234567896,101.23,1000,103.45,1000}
 *
 * All field values are generated in the constructor so the object is fully
 * immutable by the time it is handed to the executor.
 */
public class CertificateUpdate implements Callable<String> {

    private final long timestamp;
    private final String isin;
    private final double bidPrice;
    private final int bidSize;
    private final double askPrice;
    private final int askSize;

    /**
     * Creates a {@code CertificateUpdate} with randomly generated values.
     *
     * <ul>
     *   <li>Timestamp  – current epoch millis</li>
     *   <li>ISIN       – valid 12-character ISIN (see {@link ISINGenerator})</li>
     *   <li>Bid price  – random value in [100.00, 200.00], 2 decimal places</li>
     *   <li>Bid size   – random integer in [1 000, 5 000]</li>
     *   <li>Ask price  – random value in [100.00, 200.00], 2 decimal places</li>
     *   <li>Ask size   – random integer in [1 000, 10 000]</li>
     * </ul>
     */
    public CertificateUpdate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        this.timestamp = System.currentTimeMillis();
        this.isin      = ISINGenerator.generate(random);

        // Prices: generate integer cents in [10000, 20000] → divide by 100 gives [100.00, 200.00]
        this.bidPrice  = random.nextInt(10001) / 100.0 + 100.0;
        this.bidSize   = random.nextInt(4001) + 1000;   // [1000, 5000]
        this.askPrice  = random.nextInt(10001) / 100.0 + 100.0;
        this.askSize   = random.nextInt(9001) + 1000;   // [1000, 10000]
        validate();
    }

    /**
     * Returns a comma-separated string representation of this certificate update.
     *
     * @return e.g. {@code "1352122280502,DE1234567896,101.23,1000,103.45,1000"}
     */
    @Override
    public String call() {
        return String.format("%d,%s,%.2f,%d,%.2f,%d",
                timestamp, isin, bidPrice, bidSize, askPrice, askSize);
    }

    public long getTimestamp() { return timestamp; }
    public String getIsin()    { return isin; }
    public double getBidPrice(){ return bidPrice; }
    public int getBidSize()    { return bidSize; }
    public double getAskPrice(){ return askPrice; }
    public int getAskSize()    { return askSize; }

    /**
     * Returns the CSV line for this update without throwing a checked exception.
     * Prefer this over calling {@link #call()} directly from application code.
     *
     * @return comma-separated line e.g. {@code "1352122280502,DE123456789X,101.23,1000,103.45,2000"}
     */
    public String toLine() {
        try {
            return call();
        } catch (Exception e) {
            throw new RuntimeException("Failed to format certificate update", e);
        }
    }

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
