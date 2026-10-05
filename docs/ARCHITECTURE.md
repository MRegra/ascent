# Ascent — architecture

Ascent is three layers, each a Maven module, each usable on its own. The split keeps the measurement and
orchestration logic away from the workload it happens to be running.

```
                 clients (curl, a browser, your app)
                          │  GET /solve?w&h&s&box   ·   GET /cluster
        ┌─────────────────┴───────────────────────────────┐
        │ ascent-cloud  Gateway → LoadBalancer → Worker · AutoScaler · Cloud (LocalCloud) │
        └─────────────────┬───────────────────────────────┘
                          │  RequestSpec (primitives)  ·  Measured (cost)
        ┌─────────────────┴───────────────────────────────┐
        │ ascent-meter  InstrumentingClassLoader → Counters → Counts → CostModel.weight │
        │               MetricsStore · CostEstimator                                    │
        └─────────────────┬───────────────────────────────┘
                          │  loads the solver's bytecode, instrumented
        ┌─────────────────┴───────────────────────────────┐
        │ ascent-solver HeightMap · HeightMaps · Solver (BFS/DFS/A*)                     │
        └──────────────────────────────────────────────────┘
```

## ascent-solver — the workload

A `HeightMap` is a grid of terrain heights; `Solver.solve` searches from a start cell to the highest peak in a
bounding box, by BFS, DFS or A*, where climbing costs more than descending. It's written in plain imperative loops
with explicit arrays — not because that's elegant, but because this is the code the meter instruments, and a
basic-block counter only tells you something when the control flow is real. Terrain is generated deterministically
from a seed, so the same request always does the same work — essential for reproducible measurements.

## ascent-meter — measuring and predicting cost

This is the heart, and the modern replacement for the faculty's BIT tool.

1. **Instrumentation.** `InstructionCounter` uses ASM to rewrite a class: a call to `Counters.bb()` at the start of
   every basic block (method entry and each label), and `Counters.load()` / `Counters.store()` before every
   local-variable load / store. The inserted calls are stack-neutral and ASM recomputes the stack-map frames, so the
   result verifies. `<clinit>` is left alone — one-time class setup isn't part of a request's cost.
2. **A loader, not an agent.** `InstrumentingClassLoader` loads the solver package from its bytecode through the
   transformer; everything else, including `Counters`, is delegated to the parent so the counters stay shared.
   `Counters` keeps a **thread-local** tally, so each request — which runs on its own thread — is measured on its own.
3. **A number.** `CostModel.weight(Counts)` collapses the tally to one cost, `0.7·bb + 0.1·load + 0.2·store` — the
   original project's weights, kept because they're a reasonable, explainable default.
4. **Prediction.** `CostEstimator` learns, per strategy, an average cost-per-cell from the `MetricsStore` and
   multiplies by a request's search-box area; with no history it uses a default. Every finished request records a real
   measurement, so the estimate sharpens. This is the crucial move: the balancer and scaler act *before* a request
   runs, so they need a prediction, and the prediction comes from measured history, not a guess.

`InstrumentedSolver` ties it together: it drives the instrumented solver reflectively (primitives in, a `Measured`
out), resetting the thread's counters first and snapshotting them after. Primitives cross the classloader boundary
because the solver's own types live in a different `Class` inside the instrumented world.

## ascent-cloud — routing and scaling

- **`Cloud`** is the provider seam: `launch`, `terminate`, `workers`. `LocalCloud` is an in-process simulator; a real
  AWS adapter is the same interface (roadmap). A `Worker` carries an *assigned load* — the estimated cost of requests
  routed to it but not yet finished.
- **`LoadBalancer.route`** estimates a request's cost and sends it to the worker with the least assigned load, then
  reserves that estimate on it. Cost-aware, not round-robin: a worker already carrying two expensive searches won't be
  handed a third.
- **`AutoScaler.reconcile`** compares average load per worker to a target and makes at most one change — add a worker
  above the target, remove an idle one below half the target (hysteresis so it doesn't flap) — staying within
  `[min, max]`. Call it on a timer and on each request; the pool tracks load.
- **`Gateway`** is the HTTP front door, one request per thread so the per-thread meter works, reconciling the pool as
  requests arrive.

## Why the seams are where they are

`RequestSpec`/`Measured` (primitives and a cost), `Cloud` (the provider), and `MetricsStore` (the history) are the
interfaces everything plugs into. Swapping the local simulator for real EC2, or the classloader for a `-javaagent`, or
the per-cell estimator for a regression, is a new implementation of one seam — the rest doesn't move. That's how the
roadmap items land without a rewrite.
