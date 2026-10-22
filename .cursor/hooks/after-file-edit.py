#!/usr/bin/env python3
"""Format, lint, and syntax-check the single file from an afterFileEdit hook.

Reads the documented stdin payload (file_path, edits, plus the common hook
fields). Always exits 0 so a formatter failure cannot wedge the agent.
Does not boot Maven.
"""

import json
import py_compile
import subprocess
import sys
from pathlib import Path


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


def inside(root, path):
    try:
        path.resolve().relative_to(root.resolve())
        return True
    except ValueError:
        return False


def strip_trailing_whitespace(path):
    text = path.read_text(encoding="utf-8")
    lines = [line.rstrip() for line in text.splitlines()]
    updated = "\n".join(lines)
    if text.endswith("\n") or text == "":
        updated += "\n"
    if updated != text:
        path.write_text(updated, encoding="utf-8")


def format_java(root, path):
    jar = root / ".cursor/hooks/lib/google-java-format-1.25.2-all-deps.jar"
    if not jar.is_file():
        print(f"java formatter jar missing: {jar}", file=sys.stderr)
        return
    result = subprocess.run(
        ["java", "-jar", str(jar), "--replace", str(path)],
        cwd=root,
        capture_output=True,
        text=True,
        timeout=45,
        check=False,
    )
    if result.returncode != 0:
        print(result.stderr.strip() or result.stdout.strip(), file=sys.stderr)
    else:
        print(f"formatted and parsed {path.name}", file=sys.stderr)


def lint_json(path):
    data = json.loads(path.read_text(encoding="utf-8"))
    rendered = json.dumps(data, indent=2) + "\n"
    path.write_text(rendered, encoding="utf-8")


def lint_shell(path):
    result = subprocess.run(
        ["bash", "-n", str(path)],
        capture_output=True,
        text=True,
        timeout=15,
        check=False,
    )
    if result.returncode != 0:
        print(result.stderr.strip(), file=sys.stderr)


def lint_python(path):
    py_compile.compile(str(path), doraise=True)


def handle(path, root):
    suffix = path.suffix.lower()
    if suffix == ".java":
        format_java(root, path)
        return
    if suffix == ".json":
        lint_json(path)
        return
    if suffix in {".yml", ".yaml", ".md"}:
        strip_trailing_whitespace(path)
        return
    if suffix == ".sh" or path.name in {"mvnw"}:
        lint_shell(path)
        return
    if suffix == ".py":
        lint_python(path)
        return


def main():
    raw = sys.stdin.read()
    payload = json.loads(raw) if raw.strip() else {}
    file_path = payload.get("file_path")
    if not file_path:
        print("{}")
        return
    root = repo_root(payload)
    path = Path(file_path)
    if not path.is_file() or not inside(root, path):
        print("{}")
        return
    if "target" in path.parts or ".git" in path.parts:
        print("{}")
        return
    handle(path, root)
    print("{}")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:  # noqa: BLE001 - hook must not wedge the agent
        print(f"afterFileEdit hook: {error}", file=sys.stderr)
        print("{}")
        sys.exit(0)
