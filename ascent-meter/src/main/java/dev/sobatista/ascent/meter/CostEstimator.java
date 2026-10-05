package dev.sobatista.ascent.meter;

/**
 * Predicts what a request will cost <em>before</em> running it, from the history of what similar requests cost.
 *
 * <p>The model is deliberately simple and explainable: for each strategy it learns an average cost-per-cell from
 * past measurements, and multiplies by this request's search-box area. With no history it falls back to a default
 * per-cell cost. The load balancer and autoscaler act on this estimate; every finished request feeds a real
 * measurement back, so the estimate sharpens over time.
 */
public final class CostEstimator {
    private final MetricsStore store;
    private final long defaultCostPerCell;

    public CostEstimator(MetricsStore store, long defaultCostPerCell) {
        this.store = store;
        this.defaultCostPerCell = defaultCostPerCell;
    }

    public long estimate(RequestSpec spec) {
        double sumRatio = 0;
        int n = 0;
        for (RequestMetrics m : store.all()) {
            if (m.strategy().equals(spec.strategy()) && m.boxArea() > 0) {
                sumRatio += (double) m.weight() / m.boxArea();
                n++;
            }
        }
        double perCell = n > 0 ? sumRatio / n : defaultCostPerCell;
        return Math.max(1, Math.round(perCell * spec.boxArea()));
    }
}
