---
name: pr-review-checklist
description: Use when asked to review a PR, propose review comments, or prepare a merge checklist.
---

# Review checklist
## Correctness
- Does the change match the issue/user story?
- Edge cases, nullability, threading, lifecycle.

## Tests
- New/updated unit tests exist for logic changes
- Flaky patterns avoided (timers, real network, device state)

## Architecture
- Boundaries respected (UI/Domain/Data)
- DI scope correct
- Side effects isolated

## Android specifics
- Compose: state hoisting, recomposition safety, stable types
- Coroutines: dispatcher correctness, structured concurrency
- Resources: localization, accessibility

# Deliverable
- 5-10 review comments grouped by severity: [must] [should] [nit]
- A merge-ready checklist (tests, docs, rollout notes)
