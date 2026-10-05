# Ascent — specification

This is what Ascent promises, in our own words. It targets the IST "HillClimbing@Cloud" problem; the wording is
original.

## The workload

- A **height map** is a `width × height` grid of integer terrain heights, generated deterministically from a seed.
- A **search request** is `(seed, width, height, strategy, start, box)` where `box = (x0,y0,x1,y1)` and
  `strategy ∈ {BFS, DFS, ASTAR}`. The solver finds a path from `start` to the highest cell in the box; a step's cost
  is `1 + max(0, Δheight)` (climbing is expensive, descending is cheap).
- The same request always does the same work. Work (nodes expanded) grows with the box area and varies by strategy.

## Measuring cost

- The solver's bytecode is instrumented to count, **per request (per thread)**: basic blocks entered, local-variable
  loads, and local-variable stores.
- A request's **weight** is `round(0.7·basicBlocks + 0.1·loads + 0.2·stores)`.
- Measurement is **deterministic** for a given request (after one-time class linkage), and **monotone**: a request
  that does more work measures as heavier.
- One-time class initialisation (`<clinit>`) is excluded — it is not part of a request's recurring cost.

## Predicting cost

- The estimator predicts a request's weight **before** it runs, from recorded history.
- The model: per strategy, the average `weight / boxArea` over past requests, times this request's `boxArea`; with no
  history, a configured default per-cell cost.
- Guarantees: with no history the estimate equals `default × area`; after recording a request, the estimate for an
  identical request is within a few percent of the real weight; a larger box is always estimated to cost at least as
  much as a smaller one of the same strategy.

## Routing

- The load balancer routes each request to the worker with the **least assigned load**, where assigned load is the sum
  of the estimated weights of requests routed to it but not yet finished.
- Consequence: expensive requests spread out instead of stacking on one worker; a cheap request prefers an idle
  worker over a busy one.

## Scaling

- The autoscaler keeps the worker count in `[min, max]`.
- `reconcile()` makes at most one change: it **adds** a worker when the average assigned load per worker exceeds the
  target, and **removes** an idle worker when the average drops below half the target (hysteresis), provided the
  remaining workers would still be under the target.
- Called repeatedly, it converges: the pool grows to meet a burst and shrinks back to `min` when the load drains.

## Assumptions and non-goals

- The **cloud is pluggable**; the shipped implementation is an in-process simulator. A real cloud (EC2/DynamoDB/
  CloudWatch) is a `Cloud` implementation, on the roadmap.
- The **instrumenter targets your own trusted code** (the solver package), loaded in-process. It is not a sandbox for
  untrusted code.
- The **gateway is a local demo server** — no auth, minimal input handling. Not for the open internet.
- Measurement perturbs timing (every block and load/store calls a counter); it measures *work done*, not wall-clock.
