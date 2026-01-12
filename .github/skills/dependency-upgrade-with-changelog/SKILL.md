---
name: dependency-upgrade-with-changelog
description: Use when upgrading dependencies. Produces safe upgrade plan, changelog links, and validation steps.
---

# Approach
1. Identify current version and target version (minor/patch first, then major).
2. Check breaking changes:
   - release notes / migration docs
   - deprecated APIs used in repo (search)
3. Update:
   - one major at a time
   - keep changes minimal
4. Validation:
   - run unit tests + lint
   - smoke-check key flows

# Output
- Upgrade plan (steps)
- Risk items + mitigations
- Final diff + validation commands
