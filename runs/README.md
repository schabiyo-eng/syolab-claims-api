# Run log

Each role appends one JSON object as a single line to `runs/log.jsonl` when it finishes. The stop hook appends a line with `"kind": "tests"`.

`runs/*.jsonl` and `runs/hook-state.json` are gitignored. Do not commit a run.

```json
{"ts":"ISO-8601","ticket":null,"kind":"plan|implement|review|reflect|tests","model":"model-id-or-null","retries":0,"failures":[],"reviewerFlags":[],"escalations":[],"notes":""}
```

- `kind` is `plan`, `implement`, `review`, `reflect`, or `tests` for the stop hook.
- `model` is the model id that ran, or null when the hook has no model.
- Reviewer `notes` name the evidence (command and result), not a diff summary.
- `escalations` lists roles moved to a stronger model. After two failed implementer retries the stop hook names the implementer.
