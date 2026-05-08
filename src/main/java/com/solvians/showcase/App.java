package com.solvians.showcase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entry point for the certificate feed generator.
 *
 * <p>Accepts exactly two arguments:
 * <ol>
 *   <li>Number of threads  — must be a positive integer</li>
 *   <li>Number of quotes per thread — must be a positive integer</li>
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
    public App(String threads, String quotes) {
    }

    public static void main(String[] args) {
        // Step 1 — validate argument count
        if (args.length < 2) {
            throw new RuntimeException(
                    "Expected 2 arguments: <threads> <quotesPerThread>. Got: " + args.length
            );
        }

        // Step 2 — parse (NumberFormatException surfaces naturally if not numeric)
        int threads = Integer.parseInt(args[0]);
        int quotes  = Integer.parseInt(args[1]);

        // Step 3 — validate values make logical sense
        validateArgs(threads, quotes);

        // Step 4 — build executor explicitly so we can shut it down cleanly
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(threads, Runtime.getRuntime().availableProcessors())
        );

        try {
            CertificateUpdateGenerator generator =
                    new CertificateUpdateGenerator(threads, quotes, executor);

            // Step 5 — generate and print
            generator.generateQuotes()
                    .map(CertificateUpdate::toLine)   // clean — no try/catch needed
                    .forEach(System.out::println);

        } catch (RuntimeException e) {
            System.err.println("Generation failed: " + e.getMessage());
            System.exit(1);
        } finally {
            // Step 6 — always shut down the pool, even if an exception occurred
            // prevents the JVM from hanging after main() exits
            executor.shutdown();
        }
    }

    /**
     * Validates that thread and quote counts are logically valid.
     *
     * <p>Kept as a separate method so it can be tested independently
     * and so {@code main} reads like a clear sequence of steps.
     *
     * @throws IllegalArgumentException if either value is zero or negative
     */
    static void validateArgs(int threads, int quotes) {
        if (threads <= 0 || threads > 100) {
            throw new IllegalArgumentException(
                    "threads must be between 1 and 100, got: " + threads
            );
        }
        if (quotes <= 0 || quotes > 1_000_000) {
            throw new IllegalArgumentException(
                    "quotes must be between 1 and 1,000,000, got: " + quotes
            );
        }
    }
}