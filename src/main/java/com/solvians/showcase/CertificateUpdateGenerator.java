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
 *   <li><b>Configuration</b> – how many items to produce and how many threads to use</li>
 *   <li><b>Execution</b>     – running tasks in parallel via an {@link ExecutorService}</li>
 *   <li><b>Delivery</b>      – returning results as a {@link Stream} for the caller to process</li>
 * </ul>
 *
 * <p>The {@link ExecutorService} is injected rather than created internally.
 * This means the caller controls the threading strategy and lifecycle,
 * tests can pass a controlled single-threaded executor,
 * and the threading strategy can be swapped (e.g. virtual threads in Java 21)
 * without changing this class at all.
 *
 * <p><b>Typical usage:</b>
 * <pre>{@code
 *   ExecutorService pool = Executors.newFixedThreadPool(10);
 *   CertificateUpdateGenerator generator = new CertificateUpdateGenerator(10, 100, pool);
 *   Stream<CertificateUpdate> updates = generator.generateQuotes();
 *   pool.shutdown();
 * }</pre>
 */
public class CertificateUpdateGenerator {

    private final int threads;
    private final int quotes;

    /**
     * The executor that runs certificate generation tasks in parallel.
     *
     * <p>Injected from outside so the caller owns the lifecycle (shutdown, reuse, swap).
     * This also makes the class easy to test — pass a single-threaded executor
     * for predictable, deterministic test behaviour.
     */
    private final ExecutorService executor;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Full constructor. Accepts an externally created executor.
     *
     * <p>Use this when you want full control over the threading strategy, for example:
     * <ul>
     *   <li>Sharing one pool across multiple generators</li>
     *   <li>Using virtual threads: {@code Executors.newVirtualThreadPerTaskExecutor()}</li>
     *   <li>Using a single-threaded executor in tests</li>
     * </ul>
     *
     * @param threads  number of worker threads (also used to calculate total output)
     * @param quotes   number of quotes per thread
     * @param executor the executor to run generation tasks on — caller is responsible for shutdown
     */
    public CertificateUpdateGenerator(int threads, int quotes, ExecutorService executor) {
        this.threads  = threads;
        this.quotes   = quotes;
        this.executor = executor;
    }

    /**
     * Convenience constructor. Creates a fixed thread pool internally.
     *
     * <p>Use this for straightforward cases where you don't need to share
     * or customise the executor. The pool size matches the {@code threads} parameter.
     *
     * @param threads number of worker threads
     * @param quotes  number of quotes per thread
     */
    public CertificateUpdateGenerator(int threads, int quotes) {
        this(threads, quotes, Executors.newFixedThreadPool(threads));
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates {@code threads × quotes} certificate updates in parallel.
     *
     * <p>The method orchestrates three steps:
     * <ol>
     *   <li>Build a list of tasks — one per certificate update</li>
     *   <li>Execute all tasks in parallel and wait for completion</li>
     *   <li>Return the results as a {@link Stream}</li>
     * </ol>
     *
     * <p>Each step is handled by a dedicated private method so future changes
     * have a single, obvious place to land:
     * <ul>
     *   <li>Different task type?        → change {@link #buildTasks(int)}</li>
     *   <li>Add timeout or retry logic? → change {@link #executeTasks(List)}</li>
     *   <li>Filter or transform output? → change this method</li>
     * </ul>
     *
     * @return a {@code Stream} of fully constructed {@link CertificateUpdate} objects
     */
    public Stream<CertificateUpdate> generateQuotes() {
        int total = threads * quotes;

        List<Callable<CertificateUpdate>> tasks = buildTasks(total);
        List<CertificateUpdate> results         = executeTasks(tasks);

        return results.stream();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds the list of tasks to be executed in parallel.
     *
     * <p>Each task is {@code CertificateUpdate::new} — a constructor reference
     * that acts as a {@code Callable<CertificateUpdate>}.
     * When the executor calls {@code task.call()}, it simply constructs
     * a new {@link CertificateUpdate}, which generates all random field values
     * internally in its constructor.
     *
     * <p>Creating the task list upfront (rather than submitting tasks one by one
     * inside the executor) keeps the execution step clean and makes
     * the task list easy to inspect, modify, or replace in isolation.
     *
     * @param total the number of tasks to create
     * @return a list of {@code Callable<CertificateUpdate>} ready for submission
     */
    private List<Callable<CertificateUpdate>> buildTasks(int total) {
        List<Callable<CertificateUpdate>> tasks = new ArrayList<>(total);

        for (int i = 0; i < total; i++) {
            // CertificateUpdate::new is a constructor reference —
            // equivalent to () -> new CertificateUpdate()
            // but more concise. Each call to task.call() produces one new instance.
            tasks.add(CertificateUpdate::new);
        }

        return tasks;
    }

    /**
     * Submits all tasks to the executor, waits for completion, and collects results.
     *
     * <p>{@code invokeAll} handles three things in one call:
     * <ol>
     *   <li>Submits every task to the thread pool</li>
     *   <li>Blocks until every task has finished (or the thread is interrupted)</li>
     *   <li>Returns a {@code List<Future>} where every future is already completed</li>
     * </ol>
     *
     * <p>Because {@code invokeAll} guarantees all futures are done before returning,
     * the subsequent {@code future.get()} calls return instantly — there is no
     * additional waiting at collection time.
     *
     * <p><b>Error handling:</b>
     * <ul>
     *   <li>{@code InterruptedException} — the waiting thread was interrupted externally.
     *       We restore the interrupt flag with {@code Thread.currentThread().interrupt()}
     *       so the caller can detect and handle it. We return an empty list rather
     *       than crashing, so partial results are not silently corrupted.</li>
     *   <li>{@code ExecutionException} (wrapped in RuntimeException) — a task itself
     *       threw an exception. Wrapping preserves the original cause for debugging.</li>
     * </ul>
     *
     * @param tasks the list of tasks to execute
     * @return a list of completed {@link CertificateUpdate} objects
     */
    private List<CertificateUpdate> executeTasks(List<Callable<CertificateUpdate>> tasks) {
        try {
            return executor.invokeAll(tasks)   // submit all, block until all done
                    .stream()
                    .map(future -> {
                        try {
                            // future.get() is safe here — invokeAll guarantees
                            // every future is already complete before we reach this point
                            return future.get();
                        } catch (Exception e) {
                            // Wrap checked exception so it can propagate through the stream
                            throw new RuntimeException("Failed to retrieve task result", e);
                        }
                    })
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            // The thread waiting in invokeAll was interrupted from outside.
            // Restore the interrupt flag so the caller's thread can detect it —
            // swallowing it here would hide the signal from code further up the call stack.
            Thread.currentThread().interrupt();

            // Return empty list rather than null or a partial result.
            // The caller can check if the stream is empty and decide what to do.
            return Collections.emptyList();
        }
    }
}
