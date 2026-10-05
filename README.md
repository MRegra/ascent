# Ascent

**A cost-aware load balancer and autoscaler — in Java, built to be read.**

[![CI](https://github.com/MRegra/ascent/actions/workflows/ci.yml/badge.svg)](https://github.com/MRegra/ascent/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)
![status](https://img.shields.io/badge/status-0.2.0%20(pre--1.0)-orange)

Some requests are cheap and some are brutally expensive, and from the outside they look the same. A path search over a
small patch of terrain finishes instantly; the same search over a whole mountain range churns for a while. If your
load balancer treats them as equal — round-robin, one request one slot — it piles the expensive ones onto the same
unlucky server while others idle.

Ascent **measures what a request actually costs** and acts on it. It instruments the solver's bytecode to count what
each request executes, turns that into a cost, *predicts* the cost of the next request from that history, routes by
the prediction, and **autoscales** the worker pool up and down to match the load. It's a teaching-first codebase — the
goal is that you can read it and *learn how* bytecode instrumentation, cost estimation, and elastic scaling actually
work, not just import a jar.

## 30-second demo

Requires JDK 21 and Maven.

```bash
mvn install                        # build the modules
mvn -q -pl ascent-cloud exec:java  # run the in-process demo
```

One worker learns a request's real cost, then a burst arrives and the pool grows to meet it:

```
ascent — cost-aware load balancing + autoscaling for a search workload
one worker to start; map 96x96, f=BFS/DFS/ASTAR

burst of 9 requests -> scaled up to 4 workers, load spread:
  worker-1  assigned load 891,698
  worker-2  assigned load 1,029,938
  worker-3  assigned load   552,960
  worker-4  assigned load 1,029,938

  learning:   first estimate 552,960 -> after one run 891,698 (actual 891,698)
  autoscaler: grew to 4 workers under the burst, back to 1 when it drained
  served 10 requests across the cluster. ✓
```

Notice the estimate: with no history it's a rough default; after measuring **one** real run it nails the next one.

## How a request's cost gets measured

The solver is ordinary code. The meter rewrites its bytecode as it loads, inserting a counter at the start of every
basic block and before every load/store — so running it tallies exactly what it did:

```java
InstrumentedSolver solver = new InstrumentedSolver(CostModel.DEFAULT);
Measured m = solver.measure(RequestSpec.wholeMap(42, 256, 256, "ASTAR"));

m.counts();   // Counts[basicBlocks=…, loads=…, stores=…]
m.weight();   // 0.7·bb + 0.1·loads + 0.2·stores  — one number to route and scale on
```

That weight feeds a `CostEstimator` that predicts the next request's cost, a `LoadBalancer` that sends it to the
least-loaded worker, and an `AutoScaler` that sizes the pool.

## Modules

| Module | What's inside |
|---|---|
| **ascent-solver** | The workload — deterministic height-map terrain and BFS/DFS/A* search to the highest peak in a box. Plain imperative loops, because this is the code we measure. |
| **ascent-meter** | The modern BIT — an ASM `InstrumentingClassLoader` + `Counters` that count basic blocks and load/store per request, the `CostModel` weight, a `MetricsStore`, and a `CostEstimator`. Start here. |
| **ascent-cloud** | The orchestration — a pluggable `Cloud` with a `LocalCloud` simulator, the cost-aware `LoadBalancer`, the `AutoScaler`, and an HTTP `Gateway`. |

## Run the gateway

```bash
mvn -q -pl ascent-cloud exec:java -Dexec.mainClass=dev.sobatista.ascent.cloud.Gateway -Dexec.args="8000"
# then:
curl 'http://localhost:8000/solve?w=256&h=256&s=ASTAR'
curl 'http://localhost:8000/cluster'
```

## How it works

Read [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the design, [`docs/SPEC.md`](docs/SPEC.md) for the exact cost
model and scaling rules, and [`ROADMAP.md`](ROADMAP.md) for what's next (a real AWS adapter, a `-javaagent`
instrumenter, benchmarks). Write-ups that explain the ideas live in [`docs/posts/`](docs/posts).

## Status & scope

Pre-1.0: the API can still move. This is a **teaching implementation**. The instrumentation, cost model, routing and
scaling are real and tested; the cloud is an in-process simulator (a real AWS adapter is on the roadmap), and the HTTP
gateway is a local demo server, not hardened for the open internet (see [SECURITY.md](SECURITY.md)).

## Provenance

Ascent grew out of a university cloud-computing assignment (IST, "HillClimbing@Cloud"). It is a clean-room Java
reimplementation and modernisation — the original code and the faculty BIT tool are not included — swapping BIT for
ASM and AWS for a pluggable cloud so it runs and is tested anywhere. See [PROVENANCE.md](PROVENANCE.md).

## License

[Apache-2.0](LICENSE).
