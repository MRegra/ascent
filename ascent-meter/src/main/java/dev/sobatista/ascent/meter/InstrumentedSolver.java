package dev.sobatista.ascent.meter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Runs the solver with its bytecode instrumented and reports what it cost. The solver classes are loaded through an
 * {@link InstrumentingClassLoader}, so they call into {@link Counters} as they run; this class resets the thread's
 * counters, drives the solver reflectively (primitives in, summary out), and reads the tally back.
 */
public final class InstrumentedSolver {
    private static final String PKG = "dev.sobatista.ascent.solver";

    private final CostModel costModel;
    private final Method random;
    private final Constructor<?> coordinate;
    private final Constructor<?> searchRequest;
    private final Class<?> strategyClass;
    private final Method solve;
    private final Method nodesExpanded;
    private final Method climbCost;
    private final Method path;

    public InstrumentedSolver(CostModel costModel) {
        this.costModel = costModel;
        try {
            ClassLoader loader = new InstrumentingClassLoader(PKG, InstrumentedSolver.class.getClassLoader());
            Class<?> heightMap = loader.loadClass(PKG + ".HeightMap");
            Class<?> heightMaps = loader.loadClass(PKG + ".HeightMaps");
            Class<?> coordinateClass = loader.loadClass(PKG + ".Coordinate");
            this.strategyClass = loader.loadClass(PKG + ".Strategy");
            Class<?> searchRequestClass = loader.loadClass(PKG + ".SearchRequest");
            Class<?> solver = loader.loadClass(PKG + ".Solver");
            Class<?> searchResult = loader.loadClass(PKG + ".SearchResult");

            this.random = heightMaps.getMethod("random", long.class, int.class, int.class);
            this.coordinate = coordinateClass.getConstructor(int.class, int.class);
            this.searchRequest = searchRequestClass.getConstructor(long.class, int.class, int.class,
                    strategyClass, coordinateClass, int.class, int.class, int.class, int.class);
            this.solve = solver.getMethod("solve", heightMap, searchRequestClass);
            this.nodesExpanded = searchResult.getMethod("nodesExpanded");
            this.climbCost = searchResult.getMethod("climbCost");
            this.path = searchResult.getMethod("path");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("could not wire the instrumented solver", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Measured measure(RequestSpec spec) {
        try {
            Object map = random.invoke(null, spec.seed(), spec.width(), spec.height());
            Object start = coordinate.newInstance(spec.startX(), spec.startY());
            Object strat = Enum.valueOf((Class) strategyClass, spec.strategy());
            Object request = searchRequest.newInstance(spec.seed(), spec.width(), spec.height(), strat, start,
                    spec.x0(), spec.y0(), spec.x1(), spec.y1());

            Counters.reset();
            Object result = solve.invoke(null, map, request);
            Counts counts = Counters.snapshot();

            int nodes = (int) nodesExpanded.invoke(result);
            long climb = (long) climbCost.invoke(result);
            int pathLength = ((List<?>) path.invoke(result)).size();
            return new Measured(counts, costModel.weight(counts), nodes, climb, pathLength);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("instrumented solve failed", e);
        }
    }
}
