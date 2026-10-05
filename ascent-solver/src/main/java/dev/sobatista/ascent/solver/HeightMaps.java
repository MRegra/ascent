package dev.sobatista.ascent.solver;

/**
 * Deterministic terrain generation: seeded value noise (a few summed waves plus a hashed jitter), so the same seed and
 * size always produce the same map. No randomness crosses runs — essential for reproducible cost measurements.
 */
public final class HeightMaps {
    private HeightMaps() {}

    public static HeightMap random(long seed, int width, int height) {
        int[] cells = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double nx = (double) x / width;
                double ny = (double) y / height;
                double wave = Math.sin(nx * 6.28 + seed % 7) + Math.cos(ny * 9.42 + seed % 5)
                        + 0.5 * Math.sin((nx + ny) * 12.56);
                int jitter = (int) (hash(seed, x, y) % 32);
                int h = (int) ((wave + 2.5) * 40) + jitter; // roughly 0..250
                cells[y * width + x] = Math.max(0, Math.min(255, h));
            }
        }
        return new HeightMap(width, height, cells);
    }

    private static long hash(long seed, int x, int y) {
        long h = seed * 1099511628211L;
        h ^= (x * 374761393L);
        h ^= (y * 668265263L);
        h ^= (h >>> 15);
        h *= 2246822519L;
        h ^= (h >>> 13);
        return Math.abs(h);
    }
}
