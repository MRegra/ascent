package dev.sobatista.ascent.solver;

import java.util.List;

/** The path found (empty if none), how many nodes were expanded, and the path's total climbing cost. */
public record SearchResult(List<Coordinate> path, int nodesExpanded, long climbCost) {
    public boolean found() {
        return !path.isEmpty();
    }
}
