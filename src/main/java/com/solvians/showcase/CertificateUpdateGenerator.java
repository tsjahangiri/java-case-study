package com.solvians.showcase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class CertificateUpdateGenerator {
    private final int threads;
    private final int quotes;

    public CertificateUpdateGenerator(int threads, int quotes) {
        this.threads = threads;
        this.quotes = quotes;
    }

    public Stream<CertificateUpdate> generateQuotes() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        // TODO: Implement me.
        List<CertificateUpdate> updateList = new ArrayList<CertificateUpdate>();
        for (int i = 0; i < threads * quotes; i++) {
            updateList.add(new CertificateUpdate());
        }
        return Stream.generate(CertificateUpdate::new).parallel().limit(quotes);
    }
}
