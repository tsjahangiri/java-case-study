package com.solvians.showcase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class CertificateUpdateGenerator {
    private final int threads;
    private final int quotes;

    public CertificateUpdateGenerator(int threads, int quotes) {
        this.threads = threads;
        this.quotes = quotes;
    }

    public Stream<CertificateUpdate> generateQuotes() {
        int total = threads * quotes;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<CertificateUpdate>> futures = new ArrayList<>(total);

        for (int i = 0; i < total; i++) {
            futures.add(executor.submit(CertificateUpdate::new));
        }
        executor.shutdown();

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return futures.stream().map(future -> {
            try {
                return future.get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate certificate update", e);
            }
        });
    }
}
