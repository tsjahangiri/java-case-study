package com.solvians.showcase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entry point for the certificate feed generator.
 *
 * <p>Accepts exactly two command-line arguments:
 * <ol>
 *   <li>Number of threads       — positive integer, max {@link CertificateUpdateGenerator#MAX_THREADS}</li>
 *   <li>Number of quotes per thread — positive integer, max {@link CertificateUpdateGenerator#MAX_QUOTES}</li>
 * </ol>
 *
 * <p>Generates {@code threads × quotes} certificate update lines
 * and prints each one to standard output.
 *
 * <p>Usage:
 * <pre>
 *   java App 10 100
 * </pre>
 */
public class App {

    // Kept for backward compatibility with existing tests
    public App(String threads, String quotes) {}

    public static void main(String[] args) {

        // ── Step 1: validate argument count ──────────────────────────────────
        if (args.length < 2) {
            throw new RuntimeException(
                    "Expected 2 arguments: <threads> <quotesPerThread>. Got: " + args.length
            );
        }

        // ── Step 2: parse — NumberFormatException surfaces naturally ──────────
        int threads = Integer.parseInt(args[0]);
        int quotes  = Integer.parseInt(args[1]);

        // ── Step 3: validate values are logically valid ───────────────────────
        validateArgs(threads, quotes);

        // ── Step 4: build executor explicitly so we control shutdown ──────────
        ExecutorService executor = Executors.newFixedThreadPool(
                // Cap at available cores — excess threads add overhead, not speed
                Math.min(threads, Runtime.getRuntime().availableProcessors())
        );

        // ── Step 5: generate and print ────────────────────────────────────────
        try {
            CertificateUpdateGenerator generator =
                    new CertificateUpdateGenerator(threads, quotes, executor);

            generator.generateQuotes()
                    .map(CertificateUpdate::toLine)
                    .forEach(System.out::println);

        } catch (RuntimeException e) {
            // Catch and re-surface with a clean message rather than a raw stack trace
            System.err.println("Generation failed: " + e.getMessage());
            System.exit(1);

        } finally {
            // ── Step 6: always shut down the executor ─────────────────────────
            // Without this the JVM hangs after main() exits because
            // non-daemon threads inside the pool are still alive
            executor.shutdown();
        }
    }

    /**
     * Validates that thread and quote counts are within acceptable bounds.
     *
     * <p>Upper bounds exist to prevent resource exhaustion:
     * passing 99 999 threads would crash the JVM; passing 10 billion quotes
     * would exhaust heap memory.
     *
     * <p>Extracted as a package-private static method so it can be tested
     * directly without going through {@code main}.
     *
     * @param threads number of threads
     * @param quotes  number of quotes per thread
     * @throws IllegalArgumentException if either value is out of range
     */
    static void validateArgs(int threads, int quotes) {
        if (threads <= 0 || threads > CertificateUpdateGenerator.MAX_THREADS) {
            throw new IllegalArgumentException(
                    "threads must be between 1 and "
                            + CertificateUpdateGenerator.MAX_THREADS + ", got: " + threads
            );
        }
        if (quotes <= 0 || quotes > CertificateUpdateGenerator.MAX_QUOTES) {
            throw new IllegalArgumentException(
                    "quotes must be between 1 and "
                            + CertificateUpdateGenerator.MAX_QUOTES + ", got: " + quotes
            );
        }
    }
}
