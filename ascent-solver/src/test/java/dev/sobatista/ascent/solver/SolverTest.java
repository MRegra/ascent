package dev.sobatista.ascent.solver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolverTest {
    private final HeightMap map = HeightMaps.random(42, 32, 32);

    @Test
    void findsAPathFromStartToThePeak() {
        SearchRequest req = SearchRequest.wholeMap(42, 32, 32, Strategy.BFS);
        SearchResult r = Solver.solve(map, req);
        assertTrue(r.found(), "a path should exist on a connected grid");
        assertEquals(new Coordinate(0, 0), r.path().get(0));
        assertTrue(r.nodesExpanded() > 0);
    }

    @Test
    void everyStrategyReachesTheSamePeak() {
        Coordinate bfsEnd = last(Solver.solve(map, SearchRequest.wholeMap(42, 32, 32, Strategy.BFS)));
        Coordinate dfsEnd = last(Solver.solve(map, SearchRequest.wholeMap(42, 32, 32, Strategy.DFS)));
        Coordinate aStarEnd = last(Solver.solve(map, SearchRequest.wholeMap(42, 32, 32, Strategy.ASTAR)));
        assertEquals(bfsEnd, dfsEnd);
        assertEquals(bfsEnd, aStarEnd);
    }

    @Test
    void aStarFindsANoMoreExpensiveClimbThanBfs() {
        SearchResult bfs = Solver.solve(map, SearchRequest.wholeMap(42, 32, 32, Strategy.BFS));
        SearchResult aStar = Solver.solve(map, SearchRequest.wholeMap(42, 32, 32, Strategy.ASTAR));
        assertTrue(aStar.climbCost() <= bfs.climbCost(), "A* minimises climb cost");
    }

    @Test
    void aBiggerBoxExpandsMoreNodes() {
        SearchResult small = Solver.solve(map,
                new SearchRequest(42, 32, 32, Strategy.BFS, new Coordinate(0, 0), 0, 0, 5, 5));
        SearchResult whole = Solver.solve(map, SearchRequest.wholeMap(42, 32, 32, Strategy.BFS));
        assertTrue(whole.nodesExpanded() > small.nodesExpanded(), "more terrain means more work");
    }

    private static Coordinate last(SearchResult r) {
        return r.path().get(r.path().size() - 1);
    }
}
