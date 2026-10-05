---
title: "Routing by cost, not by turn: why round-robin loses"
subtitle: "Round-robin is fair the way splitting a restaurant bill evenly is fair — right up until someone orders the lobster. (Ascent, part 4.)"
tags: [cloud, load-balancing, distributed-systems, java]
status: draft
date: 2026-10-05
series: Ascent
part: 4
---

We can measure a request's cost (part 2) and predict it before running it (part 3). Now we spend that prediction on
the first real decision: **which worker gets this request?**

## Why round-robin loses

Round-robin sends requests out in turn: one-two-three-one-two-three. It's fair the way splitting a restaurant bill
evenly is fair — perfectly, until someone at the table orders the lobster and three others had a side salad.

If requests #1, #4 and #7 all happen to be monster searches, round-robin drops all three on `worker-1` (every third
turn), while `worker-2` and `worker-3` get the salads. Same request *count*, wildly different *load*. The balancer did
its job by its own definition and still created a hotspot.

## Routing by load instead

The fix is to track, per worker, its **assigned load** — the sum of the estimated cost of requests sent to it but not
yet finished — and always send the next request to whoever's carrying the least:

```java
public Assignment route(RequestSpec spec) {
    long estimate = estimator.estimate(spec);
    Worker worker = cloud.workers().stream()
            .min(Comparator.comparingLong(Worker::load))
            .orElseThrow(() -> new IllegalStateException("no workers available"));
    worker.assign(estimate);                 // reserve this request's estimated cost on it
    return new Assignment(worker, spec, estimate);
}
```

A worker reserves a request's estimated cost the moment it's routed there, and releases it when the request finishes:

```java
Measured run(RequestSpec spec, long estimate) {
    try {
        Measured m = solver.measure(spec);   // the instrumented solve from part 2
        store.record(spec, m);               // feed the real cost back to the estimator (part 3)
        return m;
    } finally {
        assignedLoad -= estimate;            // release the reservation
    }
}
```

Now a monster request lands on the least-loaded worker, and — crucially — it makes that worker *look* busy to the next
routing decision, so the following requests flow elsewhere. The lobster goes to whoever hasn't ordered yet, and
everyone else keeps getting served.

## You can see it balance

In the demo, a burst of nine mixed requests (whole-map and half-map, BFS/DFS/A*) spreads across four workers like
this:

```
burst of 9 requests -> scaled up to 4 workers, load spread:
  worker-1  assigned load 891,698
  worker-2  assigned load 1,029,938
  worker-3  assigned load   552,960
  worker-4  assigned load 1,029,938
```

Not identical — the requests come in indivisible lumps of very different sizes, so you can't split them perfectly —
but *balanced by cost*, which is the thing that matters. No worker is carrying three lobsters. The tests nail the two
behaviours that matter: two equal requests go to different workers, and a cheap request skips a worker already loaded
with an expensive one.

## So what

Cost-aware routing turns "three requests each" into "roughly equal *work* each," which is what actually keeps tail
latency down. But routing only shares out the load you have — it can't conjure capacity. When the whole cluster is
underwater, you need *more workers*; when the burst passes, you want to stop paying for them. That's the autoscaler,
and it's the last post.

Code: **[github.com/MRegra/ascent](https://github.com/MRegra/ascent)**.
