package dev.sobatista.ascent.meter;

/** The outcome of running a request under instrumentation: its tally, its cost weight, and a summary of the result. */
public record Measured(Counts counts, long weight, int nodesExpanded, long climbCost, int pathLength) {}
