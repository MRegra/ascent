package dev.sobatista.ascent.cloud;

import dev.sobatista.ascent.meter.CostEstimator;
import dev.sobatista.ascent.meter.CostModel;
import dev.sobatista.ascent.meter.InMemoryMetricsStore;
import dev.sobatista.ascent.meter.InstrumentedSolver;
import dev.sobatista.ascent.meter.Measured;
import dev.sobatista.ascent.meter.MetricsStore;
import dev.sobatista.ascent.meter.RequestSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * An in-process showcase of the whole system: it measures a request by instrumenting the solver, learns its cost,
 * then drives a burst of mixed requests through the cost-aware load balancer while the autoscaler grows the pool to
 * match — and shrinks it again once the burst drains. No AWS, no network. (A real cloud adapter is on the roadmap.)
 */
public final class AscentDemo {
    private static final int MAP = 96;
    private static final int MIN = 1;
    private static final int MAX = 4;
    private static final long TARGET_LOAD = 500_000;
    private static final long DEFAULT_PER_CELL = 60;

    public record DemoResult(long estimateBefore, long estimateAfter, long actual,
                             int peakWorkers, int finalWorkers, int served) {}

    public static void main(String[] args) {
        DemoResult r = run();
        System.out.printf("%n  learning:   first estimate %,d -> after one run %,d (actual %,d)%n",
                r.estimateBefore(), r.estimateAfter(), r.actual());
        System.out.printf("  autoscaler: grew to %d workers under the burst, back to %d when it drained%n",
                r.peakWorkers(), r.finalWorkers());
        System.out.printf("  served %d requests across the cluster. ✓%n", r.served());
    }

    public static DemoResult run() {
        MetricsStore store = new InMemoryMetricsStore();
        InstrumentedSolver solver = new InstrumentedSolver(CostModel.DEFAULT);
        CostEstimator estimator = new CostEstimator(store, DEFAULT_PER_CELL);
        LocalCloud cloud = new LocalCloud(solver, store, MIN);
        LoadBalancer lb = new LoadBalancer(cloud, estimator);
        AutoScaler autoscaler = new AutoScaler(cloud, MIN, MAX, TARGET_LOAD);

        System.out.println("ascent — cost-aware load balancing + autoscaling for a search workload");
        System.out.println("one worker to start; map " + MAP + "x" + MAP + ", f=BFS/DFS/ASTAR\n");

        // 1. Learn: estimate a request with no history, run it (measured), estimate again.
        RequestSpec sample = RequestSpec.wholeMap(1, MAP, MAP, "BFS");
        long before = estimator.estimate(sample);
        Measured actual = lb.handle(sample);
        long after = estimator.estimate(sample);

        // 2. A burst arrives. Interleave autoscaling with routing so load drives the pool size.
        List<Assignment> inflight = new ArrayList<>();
        int peak = cloud.workers().size();
        for (RequestSpec spec : burst()) {
            autoscaler.reconcile();
            inflight.add(lb.route(spec));
            peak = Math.max(peak, cloud.workers().size());
        }
        System.out.printf("burst of %d requests -> scaled up to %d workers, load spread:%n", inflight.size(), peak);
        for (Worker w : cloud.workers()) {
            System.out.printf("  %-9s assigned load %,d%n", w.id(), w.load());
        }

        // 3. Process the burst, then let the autoscaler drain the pool back down.
        for (Assignment a : inflight) {
            a.run();
        }
        while (autoscaler.reconcile() != 0) {
            // converge back to MIN now that the load is gone
        }

        return new DemoResult(before, after, actual.weight(), peak, cloud.workers().size(), inflight.size() + 1);
    }

    private static List<RequestSpec> burst() {
        List<RequestSpec> out = new ArrayList<>();
        String[] strategies = {"BFS", "ASTAR", "DFS"};
        for (int i = 0; i < 9; i++) {
            String s = strategies[i % strategies.length];
            int span = (i % 3 == 0) ? MAP - 1 : (MAP / 2 - 1); // mix whole-map and half-map boxes
            out.add(new RequestSpec(2 + i, MAP, MAP, s, 0, 0, 0, 0, span, span));
        }
        return out;
    }
}
