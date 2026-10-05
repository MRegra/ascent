package dev.sobatista.ascent.cloud;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.sobatista.ascent.meter.CostEstimator;
import dev.sobatista.ascent.meter.CostModel;
import dev.sobatista.ascent.meter.InMemoryMetricsStore;
import dev.sobatista.ascent.meter.InstrumentedSolver;
import dev.sobatista.ascent.meter.Measured;
import dev.sobatista.ascent.meter.MetricsStore;
import dev.sobatista.ascent.meter.RequestSpec;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An HTTP front door to the cluster — the original project was a web service, and so is this. {@code GET /solve}
 * routes a search through the cost-aware load balancer and returns its measured cost as JSON; {@code GET /cluster}
 * reports the workers. The autoscaler reconciles on every request and on a timer, so the pool tracks real load.
 */
public final class Gateway {
    private final LoadBalancer lb;
    private final AutoScaler autoscaler;
    private final Cloud cloud;

    public Gateway(Cloud cloud, LoadBalancer lb, AutoScaler autoscaler) {
        this.cloud = cloud;
        this.lb = lb;
        this.autoscaler = autoscaler;
    }

    public HttpServer start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8)); // each request runs on its own thread => per-request meter
        server.createContext("/solve", this::solve);
        server.createContext("/cluster", this::cluster);
        server.start();

        ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ascent-autoscaler");
            t.setDaemon(true);
            return t;
        });
        ticker.scheduleAtFixedRate(autoscaler::reconcile, 1, 1, TimeUnit.SECONDS);
        return server;
    }

    private void solve(HttpExchange ex) throws IOException {
        try {
            Map<String, String> q = query(ex);
            int w = intParam(q, "w", 256);
            int h = intParam(q, "h", 256);
            RequestSpec spec = new RequestSpec(
                    Long.parseLong(q.getOrDefault("seed", "1")), w, h,
                    q.getOrDefault("s", "BFS").toUpperCase(),
                    intParam(q, "xs", 0), intParam(q, "ys", 0),
                    intParam(q, "x0", 0), intParam(q, "y0", 0),
                    intParam(q, "x1", w - 1), intParam(q, "y1", h - 1));
            autoscaler.reconcile();
            Assignment a = lb.route(spec);
            Measured m = a.run();
            String json = String.format(
                    "{\"worker\":\"%s\",\"weight\":%d,\"nodesExpanded\":%d,\"climbCost\":%d,\"pathLength\":%d,"
                            + "\"basicBlocks\":%d,\"loads\":%d,\"stores\":%d}",
                    a.worker().id(), m.weight(), m.nodesExpanded(), m.climbCost(), m.pathLength(),
                    m.counts().basicBlocks(), m.counts().loads(), m.counts().stores());
            respond(ex, 200, json);
        } catch (RuntimeException e) {
            respond(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void cluster(HttpExchange ex) throws IOException {
        StringBuilder sb = new StringBuilder("{\"workers\":[");
        boolean first = true;
        for (Worker wkr : cloud.workers()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(String.format("{\"id\":\"%s\",\"load\":%d,\"served\":%d}", wkr.id(), wkr.load(), wkr.served()));
        }
        sb.append("]}");
        respond(ex, 200, sb.toString());
    }

    private static Map<String, String> query(HttpExchange ex) {
        Map<String, String> params = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw != null) {
            for (String pair : raw.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    params.put(pair.substring(0, eq), pair.substring(eq + 1));
                }
            }
        }
        return params;
    }

    private static int intParam(Map<String, String> q, String key, int fallback) {
        String v = q.get(key);
        return v == null ? fallback : Integer.parseInt(v);
    }

    private static void respond(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8000;
        MetricsStore store = new InMemoryMetricsStore();
        InstrumentedSolver solver = new InstrumentedSolver(CostModel.DEFAULT);
        CostEstimator estimator = new CostEstimator(store, 60);
        LocalCloud cloud = new LocalCloud(solver, store, 1);
        Gateway gateway = new Gateway(cloud, new LoadBalancer(cloud, estimator),
                new AutoScaler(cloud, 1, 4, 2_000_000));
        gateway.start(port);
        System.out.printf("ascent gateway on http://localhost:%d  (GET /solve?w=256&h=256&s=ASTAR , GET /cluster)%n", port);
    }
}
