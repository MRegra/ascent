package dev.sobatista.ascent.cloud;

import dev.sobatista.ascent.meter.CostEstimator;
import dev.sobatista.ascent.meter.Measured;
import dev.sobatista.ascent.meter.RequestSpec;

import java.util.Comparator;

/**
 * Routes each request to the worker with the least estimated load — <em>cost-aware</em> balancing, not round-robin.
 * Because the choice uses the predicted cost of the request (and of what each worker is already carrying), one
 * expensive request doesn't get stacked onto a worker already busy with expensive ones.
 */
public final class LoadBalancer {
    private final Cloud cloud;
    private final CostEstimator estimator;

    public LoadBalancer(Cloud cloud, CostEstimator estimator) {
        this.cloud = cloud;
        this.estimator = estimator;
    }

    /** Pick a worker and reserve this request's estimated load on it. Call {@link Assignment#run()} to execute. */
    public Assignment route(RequestSpec spec) {
        long estimate = estimator.estimate(spec);
        Worker worker = cloud.workers().stream()
                .min(Comparator.comparingLong(Worker::load))
                .orElseThrow(() -> new IllegalStateException("no workers available"));
        worker.assign(estimate);
        return new Assignment(worker, spec, estimate);
    }

    /** Convenience: route and run in one step. */
    public Measured handle(RequestSpec spec) {
        return route(spec).run();
    }
}
