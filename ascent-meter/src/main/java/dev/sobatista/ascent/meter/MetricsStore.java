package dev.sobatista.ascent.meter;

import java.util.List;

/**
 * Where measured request costs are kept so the estimator can learn from them. In the cloud this is a shared,
 * durable store (the "MSS"); here the interface is tiny and the backing store is pluggable.
 */
public interface MetricsStore {
    void record(RequestSpec spec, Measured measured);

    List<RequestMetrics> all();
}
