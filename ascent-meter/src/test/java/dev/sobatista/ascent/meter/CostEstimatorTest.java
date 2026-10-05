package dev.sobatista.ascent.meter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CostEstimatorTest {
    private final InstrumentedSolver solver = new InstrumentedSolver(CostModel.DEFAULT);

    @Test
    void withoutHistoryFallsBackToTheDefault() {
        MetricsStore store = new InMemoryMetricsStore();
        CostEstimator est = new CostEstimator(store, 50);
        RequestSpec spec = RequestSpec.wholeMap(1, 20, 20, "BFS"); // area 400
        assertEquals(400 * 50, est.estimate(spec));
    }

    @Test
    void learnsRealCostFromHistory() {
        MetricsStore store = new InMemoryMetricsStore();
        CostEstimator est = new CostEstimator(store, 1);
        RequestSpec spec = RequestSpec.wholeMap(1, 32, 32, "BFS");
        Measured actual = solver.measure(spec);
        store.record(spec, actual);

        long predicted = est.estimate(spec);
        double error = Math.abs(predicted - actual.weight()) / (double) actual.weight();
        assertTrue(error < 0.05, "after seeing it once, the estimate for the same request is close: " + predicted
                + " vs " + actual.weight());
    }

    @Test
    void biggerRequestsAreEstimatedToCostMore() {
        MetricsStore store = new InMemoryMetricsStore();
        CostEstimator est = new CostEstimator(store, 10);
        RequestSpec small = new RequestSpec(1, 32, 32, "BFS", 0, 0, 0, 0, 7, 7);
        RequestSpec whole = RequestSpec.wholeMap(1, 32, 32, "BFS");
        store.record(whole, solver.measure(whole));
        assertTrue(est.estimate(whole) > est.estimate(small));
    }
}
