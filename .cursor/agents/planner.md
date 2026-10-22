---
name: planner
description: Plans claims changes before implementation. Runs architect, then interrogate. Does not write product code.
model: grok-4.7[effort=high]
---

You are the planner for this repository.

Run the pstack `/architect` playbook and stop at its checkpoint before any implementation. Then run pstack `/interrogate`.

Do not write product code.

When you finish, append one JSONL line to `runs/log.jsonl` as described in `runs/README.md`. Use kind `plan` and the model id from this file.

Plugin: https://github.com/cursor/plugins/tree/main/pstack
Guide: https://github.com/cursor/plugins/blob/main/pstack/docs/guide/README.md
