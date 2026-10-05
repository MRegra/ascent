package dev.sobatista.ascent.meter;

/**
 * The per-thread counters the instrumented code calls into. Each served request runs on its own thread, so the
 * thread-local tally <em>is</em> that request's cost. These methods are invoked once per basic block and once per
 * load/store instruction in the instrumented solver — so keep them cheap.
 *
 * <p>This class is deliberately loaded by the application classloader, not the instrumenting one, so every
 * instrumented copy of the solver increments the same counters.
 */
public final class Counters {
    private static final ThreadLocal<long[]> TALLY = ThreadLocal.withInitial(() -> new long[3]);

    private Counters() {}

    public static void bb() {
        TALLY.get()[0]++;
    }

    public static void load() {
        TALLY.get()[1]++;
    }

    public static void store() {
        TALLY.get()[2]++;
    }

    public static Counts snapshot() {
        long[] c = TALLY.get();
        return new Counts(c[0], c[1], c[2]);
    }

    public static void reset() {
        long[] c = TALLY.get();
        c[0] = 0;
        c[1] = 0;
        c[2] = 0;
    }
}
