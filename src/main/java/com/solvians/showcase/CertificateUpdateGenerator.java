package com.solvians.showcase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates {@link CertificateUpdate} objects in parallel using a configurable thread pool.
 *
 * <p>Responsibilities are deliberately separated into three layers:
 * <ul>
 *   <li><b>Configuration</b> — how many items to produce and how many threads to use</li>
 *   <li><b>Execution</b>     — running tasks in parallel via an {@link ExecutorService}</li>
 *   <li><b>Delivery</b>      — returning results as a {@link Stream} to the caller</li>
 * </ul>
 *
 * <p>The {@link ExecutorService} is injected rather than created internally so:
 * <ul>
 *   <li>The caller controls the threading strategy and lifecycle</li>
 *   <li>Tests can pass a single-threaded executor for predictable behaviour</li>
 *   <li>Threading strategy can be swapped (e.g. virtual threads) without changing this class</li>
 * </ul>
 */
public class CertificateUpdateGenerator {

    /** Maximum number of threads allowed to prevent resource exhaustion. */
    static final int MAX_THREADS = 100;

    /** Maximum number of quotes per thread allowed to prevent memory exhaustion. */
    static final int MAX_QUOTES  = 1_000_000;

    private final int             threads;
    private final int             quotes;
    private final ExecutorService executor;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Full constructor. Accepts an externally created executor.
     *
     * <p>Use this when you need full control over the threading strategy:
     * <ul>
     *   <li>Share one pool across multiple generators</li>
     *   <li>Use virtual threads: {@code Executors.newVirtualThreadPerTaskExecutor()}</li>
     *   <li>Use a single-threaded executor in tests for deterministic behaviour</li>
     * </ul>
     *
     * @param threads  number of worker threads — must be between 1 and {@value #MAX_THREADS}
     * @param quotes   number of quotes per thread — must be between 1 and {@value #MAX_QUOTES}
     * @param executor the executor to run tasks on — must not be null
     * @throws IllegalArgumentException if any argument is out of range or null
     */
    public CertificateUpdateGenerator(int threads, int quotes, ExecutorService executor) {
        validateConstructorArgs(threads, quotes, executor);
        this.threads  = threads;
        this.quotes   = quotes;
        this.executor = executor;
    }

    /**
     * Convenience constructor. Creates a fixed thread pool internally.
     *
     * <p>Thread count is capped at {@code availableProcessors()} to avoid
     * over-subscription — spinning up more threads than cores just causes
     * context switching overhead without parallelism benefit.
     *
     * <p>Threads are named ({@code cert-generator-N}) and set as daemon threads
     * so the JVM can exit cleanly even if shutdown is not called explicitly.
     *
     * @param threads number of worker threads — must be between 1 and {@value #MAX_THREADS}
     * @param quotes  number of quotes per thread — must be between 1 and {@value #MAX_QUOTES}
     * @throws IllegalArgumentException if either argument is out of range
     */
    public CertificateUpdateGenerator(int threads, int quotes) {
        this(
                threads,
                quotes,
                Executors.newFixedThreadPool(
                        // Cap at available processors — more threads than cores = overhead, not speed
                        Math.min(threads, Runtime.getRuntime().availableProcessors()),
                        r -> {
                            Thread t = new Thread(r);
                            // Named threads appear in thread dumps and monitoring tools
                            t.setName("cert-generator-" + t.getId());
                            // Daemon so the JVM does not hang if shutdown() is not called
                            t.setDaemon(true);
                            return t;
                        }
                )
        );
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates {@code threads × quotes} certificate updates in parallel.
     *
     * <p>Orchestrates three steps:
     * <ol>
     *   <li>Build a task list — one {@link Callable} per certificate update</li>
     *   <li>Execute all tasks in parallel and wait for completion</li>
     *   <li>Return results as a {@link Stream}</li>
     * </ol>
     *
     * <p>Future changes have a single obvious place to land:
     * <ul>
     *   <li>Different task type?        → change {@link #buildTasks(int)}</li>
     *   <li>Add timeout or retry logic? → change {@link #executeTasks(List)}</li>
     *   <li>Filter or transform output? → change this method</li>
     * </ul>
     *
     * @return a {@code Stream} of fully constructed {@link CertificateUpdate} objects
     *         — empty if execution was interrupted
     */
    public Stream<CertificateUpdate> generateQuotes() {
        int total = threads * quotes;

        List<Callable<CertificateUpdate>> tasks = buildTasks(total);
        List<CertificateUpdate>           results = executeTasks(tasks);

        return results.stream();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Validates constructor arguments before any state is set.
     *
     * <p>Extracted as a separate method so validation logic is testable
     * independently and {@code main} constructors stay readable.
     *
     * @throws IllegalArgumentException if any argument is invalid
     */
    private static void validateConstructorArgs(int threads, int quotes, ExecutorService executor) {
        if (threads <= 0 || threads > MAX_THREADS) {
            throw new IllegalArgumentException(
                    "threads must be between 1 and " + MAX_THREADS + ", got: " + threads
            );
        }
        if (quotes <= 0 || quotes > MAX_QUOTES) {
            throw new IllegalArgumentException(
                    "quotes must be between 1 and " + MAX_QUOTES + ", got: " + quotes
            );
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }
    }

    /**
     * Builds the list of tasks to be executed in parallel.
     *
     * <p>{@code CertificateUpdate::new} is a constructor reference —
     * equivalent to {@code () -> new CertificateUpdate()}.
     * When called by the executor, it constructs one {@link CertificateUpdate}
     * which generates all random field values internally.
     *
     * @param total number of tasks to create
     * @return list of {@code Callable<CertificateUpdate>} ready for submission
     */
    private List<Callable<CertificateUpdate>> buildTasks(int total) {
        List<Callable<CertificateUpdate>> tasks = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            tasks.add(CertificateUpdate::new);
        }
        return tasks;
    }

    /**
     * Submits all tasks to the executor, waits for completion, and collects results.
     *
     * <p>{@code invokeAll} submits every task, blocks until all complete,
     * and returns futures that are already resolved — so {@code future.get()}
     * returns instantly with no additional waiting.
     *
     * <p><b>Error handling:</b>
     * <ul>
     *   <li>{@code InterruptedException} — thread was interrupted while waiting.
     *       Interrupt flag is restored so callers can detect it.
     *       An empty list is returned and a warning is written to stderr.</li>
     *   <li>{@code ExecutionException} — a task itself threw an exception.
     *       Wrapped in {@code RuntimeException} to preserve the original cause.</li>
     * </ul>
     *
     * @param tasks list of tasks to execute
     * @return list of completed {@link CertificateUpdate} objects,
     *         empty if interrupted
     */
    private List<CertificateUpdate> executeTasks(List<Callable<CertificateUpdate>> tasks) {
        try {
            return executor.invokeAll(tasks)
                    .stream()
                    .map(future -> {
                        try {
                            // invokeAll guarantees all futures are done before
                            // we reach this point — get() returns instantly
                            return future.get();
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to retrieve task result", e);
                        }
                    })
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            // Restore the interrupt flag — swallowing it would hide the signal
            // from code higher up the call stack
            Thread.currentThread().interrupt();

            // Warn visibly — an empty stream in production is a serious issue
            System.err.println("[CertificateUpdateGenerator] WARNING: interrupted while " +
                    "waiting for tasks to complete — returning empty result");

            return Collections.emptyList();
        }
    }
}
