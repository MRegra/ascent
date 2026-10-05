package dev.sobatista.ascent.solver;

/** Which search the solver runs. They do the same job with very different amounts of work — which is the whole point. */
public enum Strategy {
    /** Breadth-first: fewest steps, explores widely. */
    BFS,
    /** Depth-first: finds a path fast, not the best one, explores deep. */
    DFS,
    /** A*: least climbing cost, guided by a heuristic, usually expands the fewest nodes. */
    ASTAR
}
