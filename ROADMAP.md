# Roadmap

Ascent is built in layers so each milestone is a self-contained thing to learn and to write about.

## Shipped in 0.2.0
- Cost-aware load balancer, autoscaler with hysteresis, a pluggable cloud with a local simulator, and an HTTP gateway.

## Shipped in 0.1.0
- The search workload, and the ASM instrumentation + cost model + metrics store + estimator that measure and predict
  a request's cost.

## Next
- **A real AWS adapter** — a `Cloud` backed by EC2 (launch/terminate), with the metrics store on DynamoDB and scaling
  signals from CloudWatch. The original ran on AWS; this brings it back without the lock-in. *(post: "Putting ascent on real metal")*
- **Real concurrency + benchmarks** — serve the burst on actual worker threads and measure how cost-aware routing beats
  round-robin, with numbers. *(post: "Round-robin vs. knowing the bill")*
- **A `-javaagent` instrumenter** — the same counting as a premain Java agent (and `retransformClasses`), so any code
  can be measured without a custom classloader. *(post: "From a classloader to a real Java agent")*
- **A richer cost model** — learn per-strategy cost from request features (box size, start height, terrain roughness)
  with a small regression instead of a single per-cell average.
- **Path rendering** — return the solved path as a PNG, like the original web page did.
- **CI/pipeline polish** — release automation, coverage, spotless/checkstyle.
