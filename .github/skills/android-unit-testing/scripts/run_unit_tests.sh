#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./github/skills/android-unit-testing/scripts/run_unit_tests.sh [gradle_task]
# Example:
#   ./.../run_unit_tests.sh testDebugUnitTest
#   ./.../run_unit_tests.sh :app:testDemoDebugUnitTest

TASK="${1:-testDebugUnitTest}"

echo "==> Running: ./gradlew ${TASK} --stacktrace"
./gradlew "${TASK}" --stacktrace

echo
echo "==> Reports (common locations):"
echo "  - <module>/build/reports/tests/"
echo "  - <module>/build/test-results/"
