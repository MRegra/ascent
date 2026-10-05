# Provenance

Ascent began as a university *Cloud Computing and Virtualization* assignment at IST ("HillClimbing@Cloud", MEIC,
2018–19). This repository is a **clean-room Java 21 reimplementation and modernisation** written from an understanding
of the problem, not a port of any prior code.

- The original group's code, the faculty's BIT bytecode-instrumentation tool, the provided solver, and the course
  datasets are **not** included here — they are third-party / course material and stay out of the public tree (kept
  locally under a git-ignored `reference/`).
- All code in this repository is original work, licensed under [Apache-2.0](LICENSE).

What Ascent modernises from the original:
- **BIT → ASM.** Bytecode instrumentation is done with a small ASM-based instrumenting classloader instead of the
  faculty's Java-7 BIT tool, but it measures the same things (basic blocks, loads, stores) with the same cost weights.
- **AWS → a pluggable cloud.** The load balancer and autoscaler talk to a `Cloud` interface with a local, in-process
  simulator, so the whole system runs and is tested without any cloud account. A real AWS adapter is on the roadmap.
- **Tested and reproducible.** Deterministic terrain and measurements, and a test suite that proves cost scales with
  work, routing follows cost, and the pool scales with load.

Ascent is its own project now: a teaching-first, cost-aware load balancer and autoscaler.
