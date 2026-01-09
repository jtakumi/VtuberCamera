---
name: android-unit-testing
description: Run Android JVM unit tests via Gradle, collect reports, and fix failures. Use when asked to run/repair unit tests or CI test failures.
---

# Scope
- JVM unit tests (Gradle `test*UnitTest`) first.
- Instrumented tests are out of scope unless explicitly requested.

# Preconditions
- Use the repo's Gradle Wrapper (`./gradlew`).
- Prefer deterministic commands; avoid “try random flags”.

# Step-by-step
## 1) Discover test tasks
1. List modules and test tasks:
   - `./gradlew tasks --all | grep -E "test.*UnitTest|:test"`
2. Identify common tasks:
   - `:app:testDebugUnitTest`
   - `testDebugUnitTest` (root aggregation if present)
   - flavors: `test<Flavor><BuildType>UnitTest`

## 2) Run unit tests
- First attempt (fast, useful output):
  - `./gradlew testDebugUnitTest --stacktrace`
- If multi-module, run the failing module task explicitly:
  - `./gradlew :<module>:testDebugUnitTest --stacktrace`

## 3) Collect evidence
- Always locate HTML report paths, e.g.:
  - `<module>/build/reports/tests/`
- Extract:
  - failing test class/method
  - assertion message
  - stacktrace root cause

## 4) Fix strategy
- Prefer fixing production code bugs vs weakening tests.
- If test is flaky:
  - remove time dependence
  - isolate coroutines/dispatchers
  - mock IO/network
- Keep changes minimal and explain tradeoffs.

## 5) Re-run and confirm
- Re-run the exact same command.
- If CI uses a different variant, also run that variant.

# Output format
- Command(s) executed
- Failures summary (table)
- Root cause
- Patch + why
- Validation commands
