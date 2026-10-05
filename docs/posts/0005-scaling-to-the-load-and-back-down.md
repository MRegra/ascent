---
title: "Scaling to the load, and the hysteresis that stops the flapping"
subtitle: "Adding servers when you're busy is easy. Removing them without thrashing — and doing it on a cost signal you can trust — is the real trick. (Ascent, part 5.)"
tags: [cloud, autoscaling, elasticity, java, distributed-systems]
status: draft
date: 2026-10-05
series: Ascent
part: 5
---

Routing (part 4) shares out the load you have. When there's simply too much of it for the servers you're running, you
need more servers — and when the rush passes, you want to stop paying for them. That's elastic autoscaling, and it's
the last piece of the project.

## Scale on cost, not on CPU%

Most autoscalers watch CPU or request count and scale on a threshold. We can do better, because from parts 2–3 we
already have something they don't: an honest, forward-looking measure of **load** — the sum of the estimated cost of
all in-flight requests. We scale on *that*.

The rule compares average load per worker to a target and makes at most one change per tick:

```java
public int reconcile() {
    int n = cloud.workers().size();
    long total = workers.stream().mapToLong(Worker::load).sum();
    long perWorker = n > 0 ? total / n : total;

    if (perWorker > targetLoadPerWorker && n < max) {
        cloud.launch();                       // busy: add a worker
        return +1;
    }
    if (n > min && perWorker < targetLoadPerWorker / 2) {
        if (n - 1 == 0 || total / (n - 1) <= targetLoadPerWorker) {
            cloud.terminate(idlest(workers));  // quiet: drop an idle one
            return -1;
        }
    }
    return 0;   // already right-sized
}
```

Call it on a timer and on every request; repeated calls converge the pool to the right size.

## The detail that matters: hysteresis

Notice the two thresholds aren't the same. It scales **up** above the target, but only scales **down** below *half*
the target. That gap is deliberate, and it's the difference between an autoscaler and a disaster.

If you scaled up and down at the *same* threshold, you'd get **flapping**: add a worker, which drops the average below
the line, so you remove it, which pushes the average back over, so you add it again — a machine blinking in and out of
existence every few seconds, each churn costing a cold start. The band between "add above X" and "remove below X/2"
means a worker has to be *comfortably* unnecessary before it's let go. Boring, and boring is the point — autoscalers
should be calm.

There's a second guard: it only removes a worker if the *remaining* ones would still be under target. No scaling down
into an instant overload.

## Watch it breathe

The demo drives a burst and lets the pool react:

```
burst of 9 requests -> scaled up to 4 workers ...
autoscaler: grew to 4 workers under the burst, back to 1 when it drained
```

One worker, a burst arrives and the load climbs, the pool grows to its max of four to absorb it, the requests drain,
and the pool settles back to one. It breathes with the traffic. The tests pin both ends: load drives it to `max`, an
idle pool shrinks to `min`, and it never steps outside `[min, max]`.

## The seam that makes it real

Everywhere above, "launch a worker" went through a `Cloud` interface:

```java
public interface Cloud {
    Worker launch();
    void terminate(Worker worker);
    List<Worker> workers();
}
```

The shipped implementation is an in-process simulator, which is why the whole thing runs and tests on a laptop with no
cloud account. But the autoscaler and load balancer never say the word "EC2." Putting ascent on real metal is a single
new implementation of that interface — `launch()` calls `RunInstances`, `terminate()` calls `TerminateInstances`,
`workers()` lists the running ones — and nothing above it changes. That's on the roadmap, and it's a clean seam, not a
rewrite.

## So what

Five posts, one idea carried all the way through: **measure what work costs, predict it, and route and scale to it.**
The original was a cloud-course project welded to one AWS account with a 2004-era instrumentation tool. Rebuilding it,
the lesson that stuck with me is how far one honest measurement goes — once you *know* a request's cost instead of
guessing it, routing and scaling stop being threshold-tuning guesswork and become arithmetic.

The whole thing — read it front to back, run the demo, hit the gateway with `curl` — is here:
**[github.com/MRegra/ascent](https://github.com/MRegra/ascent)**. Apache-2.0.
