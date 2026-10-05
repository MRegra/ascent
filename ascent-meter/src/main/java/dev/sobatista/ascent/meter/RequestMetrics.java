package dev.sobatista.ascent.meter;

/** One historical observation: for a strategy and search-box area, what a request actually cost. */
public record RequestMetrics(String strategy, long boxArea, long weight, int nodesExpanded) {}
