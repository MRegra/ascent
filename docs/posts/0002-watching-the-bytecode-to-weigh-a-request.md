---
title: "How do you weigh a request? Watch its bytecode."
subtitle: "Wall-clock time is a lie that depends on the machine. Counting basic blocks isn't. Rewriting a class to count itself, with ASM. (Ascent, part 2.)"
tags: [jvm, bytecode, asm, instrumentation, java]
status: draft
date: 2026-10-05
series: Ascent
part: 2
---

In part 1 I said the system measures a request's cost by *watching the bytecode execute*. That sounds like a stunt.
It's the most robust way to do it, and it's the heart of this project. Let me show you how.

## Why not just time it?

Because wall-clock time is noise wearing a number's clothes. The same request takes different times on a laptop and a
server, before and after the JIT warms up, with and without a noisy neighbour on the box. If you route and scale based
on timings, you're reacting to the *weather*, not the *work*.

The work itself — how many operations the code actually performs — is a property of the request, not the hardware. A
bigger search really does execute more loop iterations. So count *those*.

## The idea: make the code count itself

The faculty's original tool was BIT, a Java-7 bytecode instrumentation library. The modern equivalent is
[ASM](https://asm.ow2.io/). The plan is the same either way: before the solver runs, rewrite its `.class` files so
that every chunk of code bumps a counter as it executes.

Concretely, I count two things (the same two the original did):

- **basic blocks** — a counter at the start of each straight-line run of code, so loops that spin a million times
  register a million blocks;
- **loads and stores** — every read and write of a local variable.

The insertion is tiny. ASM hands you a `MethodVisitor`; you override a few callbacks and splice in a static call:

```java
@Override public void visitCode() {           // method entry = a basic block
    super.visitCode();
    count("bb");
}
@Override public void visitLabel(Label label) { // each label starts a new basic block
    super.visitLabel(label);
    count("bb");
}
@Override public void visitVarInsn(int opcode, int var) {
    if (opcode >= Opcodes.ILOAD && opcode <= Opcodes.ALOAD) count("load");
    else if (opcode >= Opcodes.ISTORE && opcode <= Opcodes.ASTORE) count("store");
    super.visitVarInsn(opcode, var);
}
private void count(String method) {
    mv.visitMethodInsn(INVOKESTATIC, "dev/sobatista/ascent/meter/Counters", method, "()V", false);
}
```

The inserted calls push and pop nothing, and I let ASM recompute the stack-map frames (`COMPUTE_FRAMES`), so the
rewritten class still verifies. The counters themselves are dead simple and **thread-local**, because each request is
served on its own thread, so each request's tally is its own:

```java
private static final ThreadLocal<long[]> TALLY = ThreadLocal.withInitial(() -> new long[3]);
public static void bb()   { TALLY.get()[0]++; }
public static void load() { TALLY.get()[1]++; }
public static void store(){ TALLY.get()[2]++; }
```

## Running the unchanged solver, instrumented

How do you get the rewritten classes to run without touching the solver's source? A custom classloader. It loads the
solver package through the ASM transformer and delegates everything else — including `Counters` — to its parent, so
every instrumented copy increments the same shared counters:

```java
c = name.startsWith(targetPrefix) ? defineInstrumented(name) : super.loadClass(name, false);
```

Then you reset the thread's counters, run the solve, and read the tally back out. The result:

```
Counts[basicBlocks=193853, loads=274020, stores=25267]
```

Collapse that to one number with a weighted sum — `0.7·bb + 0.1·load + 0.2·store`, the original's weights — and you
have a request's **cost**. A bigger search produces a bigger number, every time, on any machine.

## The bug that was actually the point

My first "is it deterministic?" test failed by a hair: two identical runs gave `193867` and `193853` basic blocks.
The loads and stores matched exactly, so the code ran the same path — where did 14 blocks come from?

Class initialisation. The *first* run of the solver triggers each class's one-time `<clinit>` (static field setup,
that `static final int[] DX = {...}`), and I was counting its basic blocks. The second run didn't pay that cost
because the classes were already initialised.

Which is a real insight, not just a test nuisance: **one-time setup is not part of a request's recurring cost.** So I
stopped instrumenting `<clinit>`, and the measurement became both deterministic and more honest. (Live, the first
request on a fresh worker still pays class-loading; you warm a worker before you trust its numbers.)

## So what

We can now put an honest price tag on a request by watching what its code does, independent of the hardware it does it
on. That number is the currency for everything else. Next: we can't run a request to find out what it costs — we have
to route it *first* — so we predict its cost from the history of what similar requests cost.

Code: **[github.com/MRegra/ascent](https://github.com/MRegra/ascent)**.
