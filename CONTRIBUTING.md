# Contributing to Ascent

Thanks for looking! Ascent is a teaching-first project about measuring work and scaling to it, so clear code and clear
explanations both count as contributions.

## Build & test
Requires **JDK 21** and **Maven 3.9+**.

```bash
mvn verify        # build + run all tests
mvn install       # build the modules into ~/.m2 (needed before exec:java below)
```

Run the in-process demo (no cloud, no network):

```bash
mvn -q -pl ascent-cloud exec:java
```

## How it's laid out
- `ascent-solver` — the workload: terrain and BFS/DFS/A* search. The CPU-intensive code we measure.
- `ascent-meter` — the instrumentation (ASM), the cost model, the metrics store, and the cost estimator. Start here.
- `ascent-cloud` — the cost-aware load balancer, the autoscaler, the pluggable cloud, and the HTTP gateway.

## Sending a change
1. Branch from `main`.
2. Keep the change small and focused. Add a test — a bug fix gets a test that fails without it.
3. Use [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`.
4. `mvn verify` is green.
5. Open a PR describing what and why.

By contributing you agree your work is licensed under the project's [Apache-2.0](LICENSE) license.

## Style
Readable over clever. The solver is written in plain imperative loops on purpose — it's the code the meter measures,
and a basic-block counter only means something if the control flow is real. Comments explain *why*, not *what*.
