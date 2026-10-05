package dev.sobatista.ascent.meter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstrumentedSolverTest {
    private final InstrumentedSolver solver = new InstrumentedSolver(CostModel.DEFAULT);

    @Test
    void instrumentationActuallyCountsWork() {
        Measured m = solver.measure(RequestSpec.wholeMap(42, 32, 32, "BFS"));
        assertTrue(m.counts().basicBlocks() > 0, "basic blocks counted");
        assertTrue(m.counts().loads() > 0, "loads counted");
        assertTrue(m.counts().stores() > 0, "stores counted");
        assertTrue(m.weight() > 0, "weight follows");
    }

    @Test
    void biggerSearchCostsMore() {
        Measured small = solver.measure(new RequestSpec(42, 32, 32, "BFS", 0, 0, 0, 0, 5, 5));
        Measured whole = solver.measure(RequestSpec.wholeMap(42, 32, 32, "BFS"));
        assertTrue(whole.weight() > small.weight(), "a larger search measures as more expensive");
    }

    @Test
    void measurementIsDeterministic() {
        solver.measure(RequestSpec.wholeMap(7, 48, 48, "ASTAR")); // warm up: pay one-time call-site linkage first
        Measured a = solver.measure(RequestSpec.wholeMap(7, 48, 48, "ASTAR"));
        Measured b = solver.measure(RequestSpec.wholeMap(7, 48, 48, "ASTAR"));
        assertEquals(a.counts(), b.counts(), "same input, same measured cost");
    }
}
