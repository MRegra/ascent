package dev.sobatista.ascent.cloud;

import dev.sobatista.ascent.meter.CostEstimator;
import dev.sobatista.ascent.meter.CostModel;
import dev.sobatista.ascent.meter.InMemoryMetricsStore;
import dev.sobatista.ascent.meter.InstrumentedSolver;
import dev.sobatista.ascent.meter.MetricsStore;
import dev.sobatista.ascent.meter.RequestSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoScalerTest {
    private final InstrumentedSolver solver = new InstrumentedSolver(CostModel.DEFAULT);

    @Test
    void scalesUpUnderLoadThenBackDownWhenIdle() {
        MetricsStore store = new InMemoryMetricsStore();
        LocalCloud cloud = new LocalCloud(solver, store, 1);
        LoadBalancer lb = new LoadBalancer(cloud, new CostEstimator(store, 60));
        AutoScaler autoscaler = new AutoScaler(cloud, 1, 4, 500_000);

        // Pile on a burst of expensive requests (routed, not yet run) and let the scaler react.
        for (int i = 0; i < 8; i++) {
            autoscaler.reconcile();
            lb.route(RequestSpec.wholeMap(i, 96, 96, "BFS"));
        }
        while (autoscaler.reconcile() > 0) {
            // keep scaling up
        }
        assertEquals(4, cloud.workers().size(), "load drives the pool to the max");

        // An idle pool (no reserved load) shrinks back to the minimum.
        LocalCloud idle = new LocalCloud(solver, store, 4);
        AutoScaler down = new AutoScaler(idle, 1, 4, 500_000);
        while (down.reconcile() != 0) {
            // converge down
        }
        assertEquals(1, idle.workers().size(), "an idle pool shrinks to the minimum");
    }

    @Test
    void neverExceedsMaxOrDropsBelowMin() {
        MetricsStore store = new InMemoryMetricsStore();
        LocalCloud cloud = new LocalCloud(solver, store, 2);
        AutoScaler autoscaler = new AutoScaler(cloud, 2, 3, 1);
        for (int i = 0; i < 20; i++) {
            autoscaler.reconcile();
        }
        int n = cloud.workers().size();
        assertTrue(n >= 2 && n <= 3, "pool stays within [min,max]: " + n);
    }
}
