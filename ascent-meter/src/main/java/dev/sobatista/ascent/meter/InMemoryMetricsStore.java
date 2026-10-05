package dev.sobatista.ascent.meter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** A thread-safe in-memory metrics store. The roadmap swaps in a shared/durable backend without touching callers. */
public final class InMemoryMetricsStore implements MetricsStore {
    private final List<RequestMetrics> history = new CopyOnWriteArrayList<>();

    @Override
    public void record(RequestSpec spec, Measured measured) {
        history.add(new RequestMetrics(spec.strategy(), spec.boxArea(), measured.weight(), measured.nodesExpanded()));
    }

    @Override
    public List<RequestMetrics> all() {
        return List.copyOf(history);
    }
}
