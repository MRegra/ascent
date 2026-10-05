package dev.sobatista.ascent.solver;

/** An immutable grid of terrain heights. {@code at(x,y)} is the height of a cell; movement uphill costs more. */
public final class HeightMap {
    private final int width;
    private final int height;
    private final int[] cells; // row-major, length width*height

    public HeightMap(int width, int height, int[] cells) {
        if (cells.length != width * height) {
            throw new IllegalArgumentException("cells length != width*height");
        }
        this.width = width;
        this.height = height;
        this.cells = cells;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int at(int x, int y) {
        return cells[y * width + x];
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
}
