---
name: model-routing
description: Routes claims work through planner, implementer, reviewer, and reflection. Use when starting a ticket or deciding which role runs next.
---

# Model routing

pstack supplies the playbooks. This skill only decides which role runs. Plugin: https://github.com/cursor/plugins/tree/main/pstack

Roles are pinned in `.cursor/agents/` with the `model` frontmatter field (https://cursor.com/docs/subagents).

1. Plan with the planner. It runs `/architect` (checkpoint, no product code) and then `/interrogate`.
2. Execute with the implementer. It follows `/tdd`. The implementer model is `composer-2.5[fast=false]`.
3. Always finish with the reviewer. Fresh context. Read only. Evidence only: tests and before/after proof. Never a diff-only review.
4. On a test failure, retry the implementer. After two failed retries, escalate the implementer to `grok-4.7[effort=high]`, the stronger model used by the planner, reviewer, and reflection.
5. Append one line to `runs/log.jsonl` for each role: ticket, retries, failures, reviewer flags, escalations, and the model used. See `runs/README.md`.
6. After the run, reflection fixes the guardrail, not the pull request. The lesson lands as a rule, a hook, or a CI check in a separate pull request a human reviews. Reflection does not merge it.
