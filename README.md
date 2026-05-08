# Solvians Java Case Study

## Introduction – Feed Generator

Our core business is to create financial websites for some of the top-tier financial institutions in the world. Those pages display information about financial products or so-called "certificates", sourcing from numerous data feeds in our back-end system.

Certificate updates of data feed are the most important element in our applications. A certificate update has the following properties:

* Timestamp (number of milliseconds since January 1, 1970, 00:00:00 GMT)
* ISIN (string, 2 random uppercase alphabets + 9 random alphanumeric characters + 1 check digit)
* Bid Price (random number, 2 decimal places, range between 100.00 and 200.00 inclusive)
* Bid Size (random number, 0 decimal place, range between 1,000 and 5,000 inclusive)
* Ask Price (random number, 2 decimal places, range between 100.00 and 200.00 inclusive)
* Ask Size (random number, 0 decimal place, range between 1,000 and 10,000 inclusive)
* ISIN check digit calculation:

To calculate the check digit, the following steps should be followed:

1. Convert any letters to numbers by the conversion table below, e.g. "DE123456789" will be converted to "13 14 1 2 3 4 5 6 7 8 9"
2. Starting from the rightmost digit, every other digit is multiplied by two. In the example, we will have "2 3 2 4 2 2 6 4 10 6 14 8 18".
3. Add up the resulting string of digits (numbers greater than 9 becoming two separate digits). In the example, we will have 2+3+2+4+2+2+6+4+1+0+6+1+4+8+1+8 = 54.
4. Subtract the sum from the smallest multiple of 10 which is greater than or equal to it, in the example we will have 60 – 54 = 6.

Conversion table for characters is:

```
A = 10  F = 15  K = 20  P = 25  U = 30  Z = 35
B = 11  G = 16  L = 21  Q = 26  V = 31
C = 12  H = 17  M = 22  R = 27  W = 32
D = 13  I = 18  N = 23  S = 28  X = 33
E = 14  J = 19  O = 24  T = 29  Y = 34
```

---

## Implementation – Feed Generator

* Write an ISIN Generator class with Unit tests to generate the ISIN string. This is the most central part of the case study.

* Write a `Callable<String>` class with Unit test to generate one line of certificate update, in which the described properties are comma separated (thousand-separator is not needed):

  `1352122280502,DE1234567896,101.23,1000,103.45,1000`

* Generating random numbers is not important in this implementation, you can write a very simple generator or use the following sample:

```java
ThreadLocalRandom random = ThreadLocalRandom.current();
```

* Use the `App` main class to
  - Take 2 parameters: (a) Number of threads, and (b) Number of certificate updates
  - According to the parameters, trigger the certificate generations in multiple threads, and
  - Finally, collect and print the lines of generated certificate updates

---

## Solution Overview

### How to build and run

```bash
# Build and run all tests
mvn test

# Run the generator directly (10 threads, 100 quotes per thread = 1000 lines)
mvn package
java -cp target/java-case-study-1.0.0.jar com.solvians.showcase.App 10 100
```

### Project structure

```
src/
├── main/java/com/solvians/showcase/
│   ├── ISINGenerator.java               # ISIN generation + check digit algorithm
│   ├── CertificateUpdate.java           # Data model + Callable<String>
│   ├── CertificateUpdateGenerator.java  # Multi-threaded generation orchestrator
│   └── App.java                         # Entry point
└── test/java/com/solvians/showcase/
    ├── ISINGeneratorTest.java
    ├── CertificateUpdateTest.java
    ├── CertificateUpdateGeneratorTest.java
    └── AppTest.java
```

---

## Design Decisions

### ISINGenerator

The check digit algorithm is implemented exactly as specified — a Luhn-style computation
where letters are expanded to their numeric values (A=10 through Z=35), digits are
flattened into a string, alternating digits from the right are doubled, and the
sum of individual digits is used to derive the check digit.

The key formula `(10 - (sum % 10)) % 10` handles both the normal case and the
edge case where the sum is already a multiple of 10 — without the outer `% 10`,
a sum of 60 would incorrectly return 10 instead of 0.

`generate(Random random)` accepts the parent type `Random` rather than
`ThreadLocalRandom` so any random source can be passed — a seeded `new Random(42)`
for deterministic test output, or `SecureRandom` for cryptographic use cases.
`ThreadLocalRandom` (preferred in multi-threaded contexts) is used by default
via the no-argument `generate()` method.

Input to `computeCheckDigit` is normalised to uppercase before processing
so callers are not penalised for passing lowercase input.

### CertificateUpdate

Implements `Callable<String>` as required. All field values are generated in
the constructor so the object is fully immutable by the time it is returned
to the caller. Immutability means instances are safe to read from multiple
threads simultaneously without synchronisation.

`toLine()` is provided alongside `call()` as a convenience wrapper that does
not throw a checked exception. This keeps stream pipelines and application
code clean — `App` uses `.map(CertificateUpdate::toLine)` rather than wrapping
every call site in a try/catch block.

`equals()`, `hashCode()`, and `toString()` are implemented so instances
behave correctly in collections and produce readable output during debugging.

A private `validate()` method is called at the end of the constructor as a
fail-fast guard — if any generated field is outside its specified range the
exception is thrown immediately rather than producing a corrupt CSV line
that would fail silently downstream.

### CertificateUpdateGenerator

The `ExecutorService` is injected rather than created internally. This means:

- The caller controls the threading strategy and lifecycle
- Tests can pass `Executors.newSingleThreadExecutor()` for predictable, deterministic behaviour
- The threading strategy can be swapped without modifying this class — for example,
  replacing `newFixedThreadPool` with `newVirtualThreadPerTaskExecutor()` in Java 21
  requires only changing the injection site, not the generator itself

The convenience constructor caps the thread pool size at `availableProcessors()`
to avoid over-subscription — spinning up more threads than CPU cores available
adds context-switching overhead without parallelism benefit.

Threads are named `cert-generator-N` and configured as daemon threads.
Named threads appear in thread dumps and monitoring tools, making problems
easier to diagnose in production. Daemon threads allow the JVM to exit cleanly
even if `shutdown()` is not called explicitly.

`invokeAll` is used instead of a manual submit loop. It submits all tasks,
blocks until every task completes, and returns futures that are already resolved —
so the subsequent `future.get()` calls return instantly with no additional waiting.

Responsibilities are separated into three private methods so future changes
have a single obvious place to land:

| Change needed | Method to modify |
|---|---|
| Different task type | `buildTasks()` |
| Add timeout or retry logic | `executeTasks()` |
| Filter or transform output | `generateQuotes()` |

### App — bug fix

The original `App.main` had a bug where the `throw new RuntimeException(...)` at
the bottom of the method fired unconditionally — even when valid arguments were
provided — because there was no `return` statement after the successful execution
block. This has been fixed.

Upper and lower bound validation was added to `validateArgs` to prevent resource
exhaustion from absurdly large inputs. The bounds are defined as constants on
`CertificateUpdateGenerator` (`MAX_THREADS = 100`, `MAX_QUOTES = 1,000,000`)
so both `App` and tests reference the same values — no magic numbers.

---

## Testing Approach

Tests are organised into groups within each test class so the purpose
of every test is immediately clear to a reviewer:

| Group | Purpose |
|---|---|
| Algorithm correctness | Known inputs verified against hand-calculated expected outputs |
| Format / structure | Structural rules checked independently of algorithm logic |
| Input validation | Null, wrong length, invalid characters, zero/negative values |
| Edge cases | Boundary values, multiple-of-ten sum edge case in check digit |
| Regression load tests | High-volume runs (10,000 iterations) to catch non-deterministic bugs |

The check digit edge case deserves specific mention: when the digit sum is
exactly a multiple of 10, the formula `(10 - sum % 10) % 10` must return `0`
not `10`. This is explicitly tested with `computeCheckDigit("00000000000")`.

Load tests run 10,000 iterations in a tight loop. These are cheap to run
(no I/O, no sleep) but reliably expose bugs that only appear for specific
character combinations or under concurrent access — the kind of bug that
passes 10 unit tests but fails in production after a few million executions.

The generator's `executeTasks` method is tested with both a `SingleThreadExecutor`
and a `CachedThreadPool` via the injected executor constructor, verifying that
correct output count is independent of the threading strategy.
