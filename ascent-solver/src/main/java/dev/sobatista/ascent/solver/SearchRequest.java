package dev.sobatista.ascent.solver;

/**
 * One job for the solver: generate terrain from {@code seed} at {@code width}x{@code height}, then search from
 * {@code start} to the highest peak inside the bounding box, using {@code strategy}. The box and size govern how much
 * work the search does — which is exactly what the cost estimator learns to predict.
 */
public record SearchRequest(long seed, int width, int height, Strategy strategy,
                            Coordinate start, int x0, int y0, int x1, int y1) {

    /** A convenient whole-map request from a corner to the global peak. */
    public static SearchRequest wholeMap(long seed, int width, int height, Strategy strategy) {
        return new SearchRequest(seed, width, height, strategy, new Coordinate(0, 0), 0, 0, width - 1, height - 1);
    }
}
