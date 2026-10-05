package dev.sobatista.ascent.meter;

/**
 * Turns an instrumentation tally into a single cost "weight". The default weights — basic blocks dominate, stores
 * count more than loads — are inherited from the original project's model: {@code 0.7·bb + 0.1·load + 0.2·store}.
 */
public record CostModel(double basicBlock, double load, double store) {
    public static final CostModel DEFAULT = new CostModel(0.7, 0.1, 0.2);

    public long weight(Counts c) {
        return Math.round(basicBlock * c.basicBlocks() + load * c.loads() + store * c.stores());
    }
}
