# Claims intake factory demo

Presenter guide for a live walkthrough. The factory sits on the pstack plugin. This repo adds model routing, a run log, and the hooks and CI that act as guardrails.

Plugin: https://github.com/cursor/plugins/tree/main/pstack
Guide: https://github.com/cursor/plugins/blob/main/pstack/docs/guide/README.md

Give the agent only `docs/demo-tickets/flag-claims-over-policy-limit.md` during the live run. This guide is for you.

## Install pstack once

In Cursor:

```text
/add-plugin pstack
/setup-pstack
```

`/setup-pstack` asks which models each role should use and writes a rule later chats pick up. Start a new chat after it finishes.

## What the audience sees

A small insurance claims desk:

- The API validates a claim when it is submitted.
- The web app submits a claim and shows the validation status.
- pstack supplies the playbooks. This repo routes them: architect and interrogate to plan, tdd to build, evidence to review, and a guardrail change when a run teaches something.

The ticket in `docs/demo-tickets/` is not implemented. That is the live exercise.

## Run it locally

API repository, one terminal:

```bash
./mvnw spring-boot:run
```

API base URL: `http://localhost:8080`. Interactive API docs: `http://localhost:8080/swagger-ui.html`.

Web repository, a second terminal:

```bash
npm install
npm run dev
```

Open `http://localhost:5173`. The dev server proxies `/api` to `http://localhost:8080`.

Before the exercise, show behavior the suite already locks in:

- Chris Dalton on `POL-1004` has a claim for `10000.00`, the same as the policy limit, and the status is Accepted.
- Amira Shah on `POL-1001` already has `6000.00` and `4000.00` accepted. Submit `1.00` with both required documents. The new claim is Rejected because the open claims together pass the limit.

## How the pieces fit

| Piece | Where | Role |
| --- | --- | --- |
| pstack | `/add-plugin pstack` | Playbooks and principles |
| Planner | `.cursor/agents` | `/architect`, then `/interrogate`. No product code |
| Implementer | `.cursor/agents` | `/tdd`: failing test, then the change, then rerun |
| Reviewer | `.cursor/agents` | Evidence only. Tests and before/after proof. Does not review by reading the diff |
| Reflection | `.cursor/agents` | Fix the guardrail, not the pull request |
| Routing skill | `.cursor/skills/model-routing` | Which role runs, and when to escalate |
| Edit hook | `.cursor/hooks.json` `afterFileEdit` | Format, lint, and a fast type check on the edited file |
| Stop hook | `.cursor/hooks.json` `stop` | Run the suite. On failure, send a follow-up. Caps the loop |
| Run log | `runs/` | Ticket, retries, failures, reviewer flags, escalations, model per role |
| CI | `.github/workflows/ci.yml` | The same suite, on every pull request |

Trust is verification. A green claim without a command and its output does not count. That is pstack's prove-it-works principle: check the real artifact.

## Live script

1. Ticket in. Open `docs/demo-tickets/flag-claims-over-policy-limit.md`. It asks for a clearer over-limit finding and two tests: well over the limit, and comfortably under.
2. Coordinator splits the work with the model-routing skill. The planner runs `/architect` and stops at the checkpoint, then `/interrogate` tries to break the design. The implementer follows `/tdd` and writes those two tests before the production change. The reviewer runs after, in a fresh context, and does not edit.
3. Validation loop. The stop hook runs the full suite, not only the new tests. A literal reading of the ticket fails two existing tests:
   - `acceptsClaimExactlyAtCoverageLimit` — an amount equal to the limit is accepted today. The ticket treats reaching the limit as over the limit.
   - `rejectsWhenOpenClaimsTogetherExceedTheCoverageLimit` — coverage is the running total of claims that were not rejected. The ticket compares only the new amount with the full policy limit.
4. The new tests still pass. Show that output. A change can agree with the tests it just added and break a boundary the suite already owns.
5. The reviewer does not walk the diff. They show before and after: those two tests passing on the current mainline behavior, then failing after the literal change, while the ticket's two tests stay green. No output means the review is inconclusive.
6. The hook sends the failure back. The run log records the failure, the retry, and the models. After two failed retries the follow-up tells the agent to escalate the implementer to a stronger model. The hook loop limit stops a runaway retry.
7. Reflection fixes the guardrail, not the pull request. The lesson is "add a boundary test for every new rule." It lands as a rule, a hook, or a CI check in its own pull request. A person reviews that pull request. The agent does not merge it, and it does not sneak the lesson into the product change.
8. Human approves the guardrail. The next rule is planned with a boundary test up front, including the equals case and any total the rule accumulates. That rule passes on the first run.

## What to say if the first run stays green

Ask for a literal pass: implement the ticket's acceptance criteria and its two tests, and leave the existing test methods' expectations as they are. Then let the stop hook run the whole suite. The prepared gap is the ticket text, not a disabled test.

## Customer-safe

This demo uses fictional policyholders and the Syolab product name. Do not add other vendors, internal identifiers, or private metrics to the script.
