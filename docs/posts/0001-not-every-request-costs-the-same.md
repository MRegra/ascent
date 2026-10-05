---
title: "Not every request costs the same, so why does your load balancer pretend they do?"
subtitle: "I rebuilt a cloud-computing college project. It starts with a quiet lie that round-robin tells. (Ascent, part 1.)"
tags: [cloud, load-balancing, autoscaling, java, performance]
status: draft
date: 2026-10-05
series: Ascent
part: 1
---

Another old university project, dug up and rebuilt from scratch in Java — because the best way I know to understand
something is to build it and then explain it to you. This one is about the cloud, and it starts with a lie your load
balancer probably tells.

## The lie

Picture a service that solves a search problem. A request says: here's a patch of mountainous terrain, find me a path
from this corner up to the highest peak. Some requests are a tiny 8×8 box — done before you blink. Some are a
1024×1024 range — the search grinds for a while.

From the outside, those two requests look *identical*. Same URL, same shape, a few different numbers. So a plain
round-robin load balancer sends them out one-two-one-two across your servers, treating each as one unit of work.

And that's the lie. Request #3 might be a thousand times heavier than request #2. Round-robin will cheerfully stack
three monsters onto `server-1` while `server-2` twiddles its thumbs, because it counted *three requests each* and
called it balanced. Your p99 latency quietly falls off a cliff and the dashboard says everything's fine.

## The fix, in one sentence

Stop counting requests. Count *cost*.

If you knew that request #3 was going to be a thousand times heavier, you'd route it differently — to an idle server,
not onto the pile. And if a burst of heavy requests showed up, you'd spin up more servers *because the load warrants
it*, not because some fixed CPU threshold happened to trip thirty seconds too late.

That's the whole project. It has three honest questions in it, and each is a post:

1. **How much did a request actually cost?** You can't measure a number you don't have, and "wall-clock time" is a
   terrible answer (it depends on the machine, the JIT, the neighbours). The project's answer is wonderful and a
   little mad: *watch the bytecode execute.* That's the next post.
2. **How much will the *next* one cost?** You have to route it before you run it, so you need a prediction — learned
   from the history of what similar requests cost. (Part 3.)
3. **Given all that, where do you send it, and how many servers do you run?** The cost-aware load balancer (part 4)
   and the autoscaler (part 5).

## What it looks like

Here's the demo, in-process, no cloud account needed. One worker learns a request's real cost; then a burst arrives:

```
  learning:   first estimate 552,960 -> after one run 891,698 (actual 891,698)
  autoscaler: grew to 4 workers under the burst, back to 1 when it drained
  served 10 requests across the cluster. ✓
```

Look at that first line. Before it had ever run the request, the system *guessed* its cost — a rough default. After
running it exactly once and measuring, its next guess was `891,698` against an actual of `891,698`. It didn't time
anything. It counted what the code did. That's where we start next.

## So what

The original was a cloud-computing course project, bolted to a specific AWS account with a Java-7 instrumentation tool
from 2004. I rebuilt it in modern Java with the cloud swapped for a pluggable interface (so it runs anywhere) and the
ancient instrumenter swapped for ASM — but the core idea is unchanged and, honestly, it should be more famous than it
is: **route and scale by what things cost, and measure cost instead of guessing it.**

The code is here: **[github.com/MRegra/ascent](https://github.com/MRegra/ascent)**. Apache-2.0, built to be read.
