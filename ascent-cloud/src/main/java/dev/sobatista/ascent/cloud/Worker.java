package dev.sobatista.ascent.cloud;

import dev.sobatista.ascent.meter.InstrumentedSolver;
import dev.sobatista.ascent.meter.Measured;
import dev.sobatista.ascent.meter.MetricsStore;
import dev.sobatista.ascent.meter.RequestSpec;

/**
 * A compute instance. It carries an <em>assigned load</em> — the sum of the estimated cost of requests routed to it
 * but not yet finished — which is what the load balancer and autoscaler reason about. Running a request measures it
 * (instrumented), records the real cost, and releases the estimate.
 */
public final class Worker {
    private final String id;
    private final InstrumentedSolver solver;
    private final MetricsStore store;
    private long assignedLoad;
    private int served;

    Worker(String id, InstrumentedSolver solver, MetricsStore store) {
        this.id = id;
        this.solver = solver;
        this.store = store;
    }

    public String id() {
        return id;
    }

    public synchronized long load() {
        return assignedLoad;
    }

    public synchronized int served() {
        return served;
    }

    synchronized void assign(long estimate) {
        assignedLoad += estimate;
    }

    Measured run(RequestSpec spec, long estimate) {
        try {
            Measured m = solver.measure(spec);
            store.record(spec, m);
            synchronized (this) {
                served++;
            }
            return m;
        } finally {
            synchronized (this) {
                assignedLoad -= estimate;
            }
        }
    }
}
