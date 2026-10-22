# Syolab Claims API

Claims intake API for the Syolab insurance demo. Adjusters submit a claim, and the API stores it with a validation status.

The store is in memory. Data resets when the process restarts.

## Run

Java 17 or newer.

```bash
./mvnw spring-boot:run
```

API base URL: `http://localhost:8080`

- Interactive docs: `http://localhost:8080/swagger-ui.html`
- OpenAPI document from the running app: `http://localhost:8080/v3/api-docs`
- Contract file the web app generates from: `openapi/claims-api.yaml`

## Tests

```bash
./mvnw -B test
```

## Demo

`DEMO.md` is the presenter guide. The ticket in `docs/demo-tickets/flag-claims-over-policy-limit.md` is intentionally not implemented. That file is the live exercise.

## Software factory

This repo uses the [pstack](https://github.com/cursor/plugins/tree/main/pstack) plugin for the engineering loop. pstack's skills stay in the plugin. This repo adds model routing, a run log, hooks, and CI around that loop.

Guide: https://github.com/cursor/plugins/blob/main/pstack/docs/guide/README.md

Install pstack in Cursor:

```text
/add-plugin pstack
/setup-pstack
```

Start a new chat after setup finishes.

| Piece | Where | What it runs |
| --- | --- | --- |
| Planner | `.cursor/agents/planner.md` | pstack `/architect`, then `/interrogate`. No product code |
| Implementer | `.cursor/agents/implementer.md` | pstack `/tdd` |
| Reviewer | `.cursor/agents/reviewer.md` | Evidence only. Tests and before/after proof. Read only |
| Reflection | `.cursor/agents/reflection.md` | A guardrail change in its own pull request |
| Routing | `.cursor/skills/model-routing/SKILL.md` | Which role runs, and when to escalate |
| Edit hook | `.cursor/hooks.json` `afterFileEdit` | Format, lint, and a syntax check of the edited file |
| Stop hook | `.cursor/hooks.json` `stop` | `./mvnw -B test`. On failure, sends a follow-up |
| Run log | `runs/log.jsonl` | Ticket, retries, failures, reviewer flags, escalations, model |
| CI | `.github/workflows/ci.yml` | The same `./mvnw -B test` command, on every pull request |

The stop hook's `loop_limit` is 3. After two failed runs in a row, the follow-up tells the agent to escalate the implementer to a stronger model. The log schema is in `runs/README.md`.

Subagent model pins use the `model` frontmatter field, confirmed in the [subagents](https://cursor.com/docs/subagents) docs. Planner, reviewer, and reflection use `grok-4.7[effort=high]`. The implementer uses `composer-2.5[fast=false]`. The reviewer sets `readonly: true`.

Hook shape was confirmed in the [hooks](https://cursor.com/docs/hooks) docs. `afterFileEdit` receives `file_path` and `edits`. A `stop` hook sends a follow-up by printing `followup_message`. The loop cap is the `loop_limit` field on that hook.
