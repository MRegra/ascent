package dev.sobatista.ascent.solver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Searches a height map from a start cell to the highest peak in a bounding box, by BFS, DFS or A*.
 *
 * <p>Written as plain imperative loops on purpose: this is the code the meter instruments, and a basic-block counter
 * only tells you something if the control flow is real. The amount of work — nodes expanded — scales with the box
 * size and the strategy, which is what makes a request's cost worth predicting.
 */
public final class Solver {
    private Solver() {}

    public static SearchResult solve(HeightMap map, SearchRequest req) {
        Coordinate goal = peak(map, req);
        int start = index(map, req.start().x(), req.start().y());
        return switch (req.strategy()) {
            case BFS -> frontierSearch(map, req, goal, start, false);
            case DFS -> frontierSearch(map, req, goal, start, true);
            case ASTAR -> astar(map, req, goal, start);
        };
    }

    /** The highest cell inside the bounding box — the peak we climb to. */
    private static Coordinate peak(HeightMap map, SearchRequest req) {
        int best = Integer.MIN_VALUE;
        int bx = req.x0();
        int by = req.y0();
        for (int y = req.y0(); y <= req.y1(); y++) {
            for (int x = req.x0(); x <= req.x1(); x++) {
                int h = map.at(x, y);
                if (h > best) {
                    best = h;
                    bx = x;
                    by = y;
                }
            }
        }
        return new Coordinate(bx, by);
    }

    private static SearchResult frontierSearch(HeightMap map, SearchRequest req, Coordinate goal,
                                               int start, boolean depthFirst) {
        int n = map.width() * map.height();
        int[] parent = new int[n];
        boolean[] seen = new boolean[n];
        for (int i = 0; i < n; i++) {
            parent[i] = -1;
        }
        Deque<Integer> frontier = new ArrayDeque<>();
        frontier.push(start);
        seen[start] = true;
        int goalIdx = index(map, goal.x(), goal.y());
        int expanded = 0;

        while (!frontier.isEmpty()) {
            int cur = depthFirst ? frontier.pop() : frontier.poll();
            expanded++;
            if (cur == goalIdx) {
                return reconstruct(map, parent, cur, expanded);
            }
            int cx = cur % map.width();
            int cy = cur / map.width();
            for (int d = 0; d < 4; d++) {
                int nx = cx + DX[d];
                int ny = cy + DY[d];
                if (!inBox(req, nx, ny)) {
                    continue;
                }
                int ni = index(map, nx, ny);
                if (!seen[ni]) {
                    seen[ni] = true;
                    parent[ni] = cur;
                    frontier.push(ni);
                }
            }
        }
        return new SearchResult(Collections.emptyList(), expanded, 0);
    }

    private static SearchResult astar(HeightMap map, SearchRequest req, Coordinate goal, int start) {
        int n = map.width() * map.height();
        int[] parent = new int[n];
        long[] g = new long[n];
        boolean[] closed = new boolean[n];
        for (int i = 0; i < n; i++) {
            parent[i] = -1;
            g[i] = Long.MAX_VALUE;
        }
        int goalIdx = index(map, goal.x(), goal.y());
        g[start] = 0;
        PriorityQueue<long[]> open = new PriorityQueue<>((a, b) -> Long.compare(a[1], b[1])); // {index, f}
        open.add(new long[] {start, heuristic(map, start, goal)});
        int expanded = 0;

        while (!open.isEmpty()) {
            long[] top = open.poll();
            int cur = (int) top[0];
            if (closed[cur]) {
                continue;
            }
            closed[cur] = true;
            expanded++;
            if (cur == goalIdx) {
                return reconstruct(map, parent, cur, expanded);
            }
            int cx = cur % map.width();
            int cy = cur / map.width();
            for (int d = 0; d < 4; d++) {
                int nx = cx + DX[d];
                int ny = cy + DY[d];
                if (!inBox(req, nx, ny)) {
                    continue;
                }
                int ni = index(map, nx, ny);
                if (closed[ni]) {
                    continue;
                }
                long tentative = g[cur] + stepCost(map, cx, cy, nx, ny);
                if (tentative < g[ni]) {
                    g[ni] = tentative;
                    parent[ni] = cur;
                    open.add(new long[] {ni, tentative + heuristic(map, ni, goal)});
                }
            }
        }
        return new SearchResult(Collections.emptyList(), expanded, 0);
    }

    private static SearchResult reconstruct(HeightMap map, int[] parent, int goalIdx, int expanded) {
        ArrayList<Coordinate> path = new ArrayList<>();
        int cur = goalIdx;
        while (cur != -1) {
            path.add(new Coordinate(cur % map.width(), cur / map.width()));
            cur = parent[cur];
        }
        Collections.reverse(path);
        long climb = 0;
        for (int i = 1; i < path.size(); i++) {
            Coordinate a = path.get(i - 1);
            Coordinate b = path.get(i);
            climb += stepCost(map, a.x(), a.y(), b.x(), b.y());
        }
        return new SearchResult(path, expanded, climb);
    }

    private static long stepCost(HeightMap map, int fx, int fy, int tx, int ty) {
        int diff = map.at(tx, ty) - map.at(fx, fy);
        return 1 + Math.max(0, diff); // flat or downhill is cheap; climbing costs the gain
    }

    private static long heuristic(HeightMap map, int idx, Coordinate goal) {
        int x = idx % map.width();
        int y = idx / map.width();
        return Math.abs(x - goal.x()) + Math.abs(y - goal.y()); // Manhattan; admissible (min step cost is 1)
    }

    private static boolean inBox(SearchRequest req, int x, int y) {
        return x >= req.x0() && x <= req.x1() && y >= req.y0() && y <= req.y1();
    }

    private static int index(HeightMap map, int x, int y) {
        return y * map.width() + x;
    }

    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DY = {0, 0, 1, -1};
}
