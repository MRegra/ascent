# Changelog

All notable changes to Ascent are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and Ascent uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Changed
- Dependency updates move from Dependabot to self-hosted Renovate (`renovate.json`, weekly grouped PRs; GitHub Dependabot
  security alerts stay on).

## [0.2.0] - 2026-10-05
### Added
- **Cost-aware load balancer** (`ascent-cloud`): routes each request to the worker with the least estimated load, so
  expensive requests don't stack up.
- **Autoscaler**: grows and shrinks the worker pool between a min and a max to track load, with hysteresis.
- **Pluggable cloud** with an in-process `LocalCloud` simulator, so the whole system runs and is tested without AWS.
- **HTTP gateway** (`/solve`, `/cluster`) and an in-process `AscentDemo`.

## [0.1.0] - 2026-10-05
### Added
- `ascent-solver`: deterministic height-map terrain and BFS/DFS/A* path search to a peak — the CPU-intensive workload.
- `ascent-meter`: an ASM instrumenting classloader that counts basic blocks and load/store instructions per request
  (the modern stand-in for the faculty BIT tool), a cost model (`0.7·bb + 0.1·load + 0.2·store`), a metrics store, and
  a cost estimator that predicts a request's cost from history.
- 10 tests covering search correctness, that measured cost scales with work and is deterministic, and that the
  estimate sharpens with history.

[Unreleased]: https://github.com/MRegra/ascent/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/MRegra/ascent/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/MRegra/ascent/releases/tag/v0.1.0
