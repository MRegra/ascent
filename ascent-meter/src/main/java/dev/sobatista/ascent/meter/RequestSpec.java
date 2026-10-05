package dev.sobatista.ascent.meter;

/**
 * A solver job described in primitives, so it can cross the classloader boundary into the instrumented world (where
 * the solver's own types live in a different {@code Class}). Mirrors the solver's SearchRequest.
 */
public record RequestSpec(long seed, int width, int height, String strategy,
                          int startX, int startY, int x0, int y0, int x1, int y1) {
    public static RequestSpec wholeMap(long seed, int width, int height, String strategy) {
        return new RequestSpec(seed, width, height, strategy, 0, 0, 0, 0, width - 1, height - 1);
    }

    /** A small key that groups "similar" requests for cost estimation: strategy and the search-box area. */
    public long boxArea() {
        return (long) (x1 - x0 + 1) * (y1 - y0 + 1);
    }
}
