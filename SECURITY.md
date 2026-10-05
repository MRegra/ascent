# Security Policy

## Supported versions
Ascent is pre-1.0; only the latest `0.x` release gets fixes.

## Reporting a vulnerability
Please **do not** open a public issue for a vulnerability. Email **sobatistacyber@gmail.com** with details and a
proof of concept if you have one. You'll get an acknowledgement within a few days.

## Scope and threat model
Ascent is a **teaching** implementation of a cost-aware load balancer and autoscaler. The interesting, defended part
is *resource* behaviour: a request's cost is measured (not guessed), routing and scaling follow that cost, and the
pool is bounded between a min and a max so it can't scale without limit.

The HTTP gateway is a minimal demo server with no authentication or input hardening beyond basic parsing — it is for
local use, not for exposing to the internet. The instrumenting classloader runs the solver's bytecode in-process and
is meant for your own trusted code, not for measuring untrusted inputs.
