package dev.sobatista.ascent.cloud;

import dev.sobatista.ascent.meter.Measured;
import dev.sobatista.ascent.meter.RequestSpec;

/** A request reserved on a worker. Running it executes the (instrumented) solve and releases the reservation. */
public record Assignment(Worker worker, RequestSpec spec, long estimate) {
    public Measured run() {
        return worker.run(spec, estimate);
    }
}
