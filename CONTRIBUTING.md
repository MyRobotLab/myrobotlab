# Contributing to MyRobotLab

## Branching

- Develop on branches from **`develop`**.
- Open PRs against **`develop`**.
- Prefer an issue number in the branch name when available.

## For humans and agents

Start with **[`AGENTS.md`](AGENTS.md)** — architecture, safe edit surfaces, dependency rules, generated files, and verify commands.

Additional guides:

- [`doc/agent/`](doc/agent/) — dependency cookbook, domain map, hotspot map
- [`doc/GENERATED.md`](doc/GENERATED.md) — do-not-edit artifacts
- [`doc/service-life-cycle.md`](doc/service-life-cycle.md) — service lifecycle

## Verify locally

```bash
mvn test -Pagent-tests
# plus a focused test for your change:
mvn test -Dtest=org.myrobotlab.service.YourServiceTest
```

## Code review

Expect review on PRs to `develop`. Address feedback, then a maintainer merges and deletes the branch.
