package dev.sobatista.ascent.cloud;

import dev.sobatista.ascent.meter.InstrumentedSolver;
import dev.sobatista.ascent.meter.MetricsStore;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** An in-process cloud: "instances" are {@link Worker}s sharing one instrumented solver and metrics store. */
public final class LocalCloud implements Cloud {
    private final InstrumentedSolver solver;
    private final MetricsStore store;
    private final List<Worker> workers = new CopyOnWriteArrayList<>();
    private final AtomicInteger counter = new AtomicInteger();

    public LocalCloud(InstrumentedSolver solver, MetricsStore store, int initial) {
        this.solver = solver;
        this.store = store;
        for (int i = 0; i < initial; i++) {
            launch();
        }
    }

    @Override
    public Worker launch() {
        Worker w = new Worker("worker-" + counter.incrementAndGet(), solver, store);
        workers.add(w);
        return w;
    }

    @Override
    public void terminate(Worker worker) {
        workers.remove(worker);
    }

    @Override
    public List<Worker> workers() {
        return List.copyOf(workers);
    }
}
