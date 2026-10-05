package dev.sobatista.ascent.cloud;

/**
 * Grows and shrinks the worker pool to track load, between {@code min} and {@code max}. It compares the average
 * assigned load per worker to a target: above the target it adds a worker, well below it (hysteresis, half the
 * target) it removes one. {@link #reconcile()} makes at most one change; call it repeatedly to converge.
 */
public final class AutoScaler {
    private final Cloud cloud;
    private final int min;
    private final int max;
    private final long targetLoadPerWorker;

    public AutoScaler(Cloud cloud, int min, int max, long targetLoadPerWorker) {
        this.cloud = cloud;
        this.min = min;
        this.max = max;
        this.targetLoadPerWorker = targetLoadPerWorker;
    }

    /** Returns +1 if it launched a worker, -1 if it terminated one, 0 if the pool is already right-sized. */
    public int reconcile() {
        var workers = cloud.workers();
        int n = workers.size();
        long total = workers.stream().mapToLong(Worker::load).sum();
        long perWorker = n > 0 ? total / n : total;

        if (perWorker > targetLoadPerWorker && n < max) {
            cloud.launch();
            return +1;
        }
        if (n > min && perWorker < targetLoadPerWorker / 2) {
            // only scale down if the remaining workers would still be under target
            if (n - 1 == 0 || total / (n - 1) <= targetLoadPerWorker) {
                cloud.terminate(idlest(workers));
                return -1;
            }
        }
        return 0;
    }

    private static Worker idlest(java.util.List<Worker> workers) {
        Worker idlest = workers.get(0);
        for (Worker w : workers) {
            if (w.load() < idlest.load()) {
                idlest = w;
            }
        }
        return idlest;
    }
}
