package com.solvians.showcase;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

public class CertificateUpdate implements Callable<String> {

    private final long timestamp;
    private final String isin;
    private final double bidPrice;
    private final int bidSize;
    private final double askPrice;
    private final int askSize;

    public CertificateUpdate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        this.timestamp = System.currentTimeMillis();
        this.isin      = ISINGenerator.generate(random);

        // Prices: generate integer cents in [10000, 20000] → divide by 100 gives [100.00, 200.00]
        this.bidPrice  = random.nextInt(10001) / 100.0 + 100.0;
        this.bidSize   = random.nextInt(4001) + 1000;   // [1000, 5000]
        this.askPrice  = random.nextInt(10001) / 100.0 + 100.0;
        this.askSize   = random.nextInt(9001) + 1000;   // [1000, 10000]
    }

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
}
