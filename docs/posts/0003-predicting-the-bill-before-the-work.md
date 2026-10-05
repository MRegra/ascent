---
title: "Predicting the bill before you do the work"
subtitle: "You have to route a request before you run it, so you need its cost in advance. The trick: learn it from the requests you've already run. (Ascent, part 3.)"
tags: [cloud, cost-estimation, load-balancing, java, machine-learning]
status: draft
date: 2026-10-05
series: Ascent
part: 3
---

Part 2 gave us a way to measure exactly what a request cost — *after* it ran. But routing and scaling happen
*before*. You have to decide where to send a request without having run it. So you need a prediction. And the lovely
thing is you already have a training set: every request you've ever served, with its real measured cost.

## The cheapest model that works

You could reach for regression or an ML model, and eventually you might. But start with the simplest thing that could
possibly work, because it's explainable and it's shockingly good here.

Observation: a request's cost scales roughly with the *area* of terrain it searches. Twice the box, roughly twice the
work. So learn a single number per strategy — the average **cost per cell** — from history, and multiply by this
request's box area:

```java
public long estimate(RequestSpec spec) {
    double sumRatio = 0; int n = 0;
    for (RequestMetrics m : store.all()) {
        if (m.strategy().equals(spec.strategy()) && m.boxArea() > 0) {
            sumRatio += (double) m.weight() / m.boxArea();   // cost per cell, for each past request
            n++;
        }
    }
    double perCell = n > 0 ? sumRatio / n : defaultCostPerCell;   // no history? a sensible default
    return Math.max(1, Math.round(perCell * spec.boxArea()));
}
```

Keyed by strategy, because BFS, DFS and A* have genuinely different costs per cell — A* is guided and expands fewer
nodes; BFS fans out. One average each, learned separately.

## Watch it learn

This is my favourite line in the whole demo:

```
learning:   first estimate 552,960 -> after one run 891,698 (actual 891,698)
```

The *first* time it sees a 96×96 whole-map BFS, it has no history, so it falls back to the default per-cell cost:
`96×96 × 60 = 552,960`. A guess. Then it actually runs that request, measures the real weight — `891,698` — and
records it. The *very next* estimate for the same request is `891,698`, dead on, because now there's one real data
point and the per-cell average is the truth.

One observation and the model snaps to reality. That's the payoff of measuring instead of guessing: your training data
is exact, so even a one-line model gets sharp fast. The tests pin this down — after recording a request, the estimate
for an identical one is within a few percent; a bigger box is always estimated to cost more.

## The feedback loop

Here's the shape of the whole system, and why it gets *better* the more it runs:

```
estimate (from history) ──► route & run ──► measure (instrument) ──► record ──┐
      ▲                                                                        │
      └────────────────────────────────────────────────────────────────────── ┘
```

Every served request is both a thing you did *and* a training example for the next decision. The estimator is never
"trained" and then frozen; it's learning continuously from production traffic, which is exactly the traffic it needs
to predict. A cold system guesses; a warm one knows.

## So what

We can now put a price on a request we haven't run yet, and that price improves itself. That unlocks the two decisions
this whole project exists to make well: *where* does this request go, and *how many* servers do we need? Next post:
the load balancer that routes by cost — and why that beats round-robin even when round-robin looks fair.

Code: **[github.com/MRegra/ascent](https://github.com/MRegra/ascent)**.
