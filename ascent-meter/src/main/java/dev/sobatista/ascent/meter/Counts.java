package dev.sobatista.ascent.meter;

/** The raw instrumentation tally for one request: basic blocks entered, local-variable loads, and stores. */
public record Counts(long basicBlocks, long loads, long stores) {
    public static final Counts ZERO = new Counts(0, 0, 0);
}
