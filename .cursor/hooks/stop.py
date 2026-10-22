#!/usr/bin/env python3
"""Stop hook: run the API suite, log the result, and follow up on failure.

Stdin is the documented stop payload (status, loop_count, plus the common
hook fields). On failure, stdout is a JSON object with followup_message.
The loop cap is loop_limit on this hook in .cursor/hooks.json.
Always exits 0 so a hook bug cannot wedge the agent.
"""

import json
import subprocess
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

COVERAGE_TESTS = (
    "acceptsClaimExactlyAtCoverageLimit",
    "rejectsWhenOpenClaimsTogetherExceedTheCoverageLimit",
)
ESCALATION_MODEL = "grok-4.7[effort=high]"


def repo_root(payload):
    roots = payload.get("workspace_roots") or []
    for root in roots:
        path = Path(root)
        if (path / "mvnw").is_file():
            return path
    here = Path(__file__).resolve()
    for parent in here.parents:
        if (parent / "mvnw").is_file():
            return parent
    return Path.cwd()


def load_failures(root):
    failures = []
    reports = root / "target" / "surefire-reports"
    if not reports.is_dir():
        return failures
    for report in sorted(reports.glob("TEST-*.xml")):
        try:
            tree = ET.parse(report)
        except ET.ParseError:
            continue
        for case in tree.findall(".//testcase"):
            if case.find("failure") is None and case.find("error") is None:
                continue
            classname = case.get("classname") or report.stem
            name = case.get("name") or "unknown"
            failures.append(f"{classname}.{name}")
    return failures


def tail(text, lines=40):
    kept = text.splitlines()[-lines:]
    clipped = [line[:240] for line in kept]
    return "\n".join(clipped)


def read_count(path):
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
        return int(data.get("consecutiveFailures", 0))
    except (OSError, json.JSONDecodeError, TypeError, ValueError):
        return 0


def followup(failures, log_tail, consecutive):
    listed = "\n".join(f"- {name}" for name in failures) or "- ./mvnw -B test failed before a test name was reported"
    message = (
        "./mvnw -B test failed. Fix the product code. Do not weaken existing tests, "
        f"including {COVERAGE_TESTS[0]} and {COVERAGE_TESTS[1]}.\n\n"
        f"Failing tests:\n{listed}\n\n"
        f"Log tail:\n{log_tail}"
    )
    if consecutive >= 2:
        message += (
            f"\n\nConsecutive test failures: {consecutive}. Escalate the implementer to a "
            f"stronger model than the fast default ({ESCALATION_MODEL}), per the model-routing skill."
        )
    return message


def append_log(path, payload, failures, consecutive, passed):
    entry = {
        "ts": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "ticket": None,
        "kind": "tests",
        "model": payload.get("model") or payload.get("model_id") or None,
        "retries": 0,
        "failures": failures,
        "reviewerFlags": [],
        "escalations": ["implementer"] if not passed and consecutive >= 2 else [],
        "notes": "./mvnw -B test passed" if passed else f"./mvnw -B test failed; consecutiveFailures={consecutive}",
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(entry, separators=(",", ":")) + "\n")


def main():
    raw = sys.stdin.read()
    payload = json.loads(raw) if raw.strip() else {}
    root = repo_root(payload)
    state_path = root / "runs" / "hook-state.json"
    log_path = root / "runs" / "log.jsonl"

    result = subprocess.run(
        ["./mvnw", "-B", "test"],
        cwd=root,
        capture_output=True,
        text=True,
        timeout=840,
        check=False,
    )
    output = (result.stdout or "") + "\n" + (result.stderr or "")
    failures = load_failures(root)
    passed = result.returncode == 0 and not failures
    if passed:
        consecutive = 0
    else:
        consecutive = read_count(state_path) + 1
        if not failures:
            failures = ["./mvnw -B test"]

    state_path.parent.mkdir(parents=True, exist_ok=True)
    state_path.write_text(json.dumps({"consecutiveFailures": consecutive}, indent=2) + "\n", encoding="utf-8")
    append_log(log_path, payload, [] if passed else failures, consecutive, passed)

    if passed:
        print("{}")
        return
    print(json.dumps({"followup_message": followup(failures, tail(output), consecutive)}))


if __name__ == "__main__":
    try:
        main()
    except Exception as error:  # noqa: BLE001 - hook must not wedge the agent
        print(f"stop hook: {error}", file=sys.stderr)
        print("{}")
        sys.exit(0)
