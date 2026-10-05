package dev.sobatista.ascent.cloud;

import dev.sobatista.ascent.meter.CostEstimator;
import dev.sobatista.ascent.meter.CostModel;
import dev.sobatista.ascent.meter.InMemoryMetricsStore;
import dev.sobatista.ascent.meter.InstrumentedSolver;
import dev.sobatista.ascent.meter.MetricsStore;
import dev.sobatista.ascent.meter.RequestSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LoadBalancerTest {
    private final InstrumentedSolver solver = new InstrumentedSolver(CostModel.DEFAULT);

    private LoadBalancer twoWorkerBalancer() {
        MetricsStore store = new InMemoryMetricsStore();
        LocalCloud cloud = new LocalCloud(solver, store, 2);
        return new LoadBalancer(cloud, new CostEstimator(store, 10));
    }

    @Test
    void spreadsEqualRequestsAcrossWorkers() {
        LoadBalancer lb = twoWorkerBalancer();
        RequestSpec spec = RequestSpec.wholeMap(1, 32, 32, "BFS");
        Assignment a = lb.route(spec);
        Assignment b = lb.route(spec);
        assertNotEquals(a.worker().id(), b.worker().id(), "the second request avoids the now-loaded worker");
    }

    @Test
    void doesNotPileAnExpensiveRequestOntoABusyWorker() {
        LoadBalancer lb = twoWorkerBalancer();
        Assignment big = lb.route(RequestSpec.wholeMap(1, 64, 64, "BFS"));   // loads one worker heavily
        Assignment small = lb.route(new RequestSpec(1, 64, 64, "BFS", 0, 0, 0, 0, 3, 3)); // cheap
        assertNotEquals(big.worker().id(), small.worker().id(), "cheap request goes to the idle worker");
        assertSame(small.worker(), small.worker());
    }
}
