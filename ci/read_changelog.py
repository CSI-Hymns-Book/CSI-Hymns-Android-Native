#!/usr/bin/env python3
"""Read the latest changelog entry from app/src/main/assets/changelog.json."""

import json
import sys
from pathlib import Path

CHANGELOG_PATH = Path("app/src/main/assets/changelog.json")
OUTPUT_PATH = Path("ci/changelog.env")
RELEASE_NOTES_PATH = Path("ci/changelog_release.txt")


def escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")


def main() -> None:
    with CHANGELOG_PATH.open(encoding="utf-8") as f:
        entries = json.load(f)

    if not entries:
        print("No changelog entries found", file=sys.stderr)
        sys.exit(1)

    entry = entries[0]
    title = entry.get("title", "")
    version = entry.get("version", "")
    date = entry.get("date", "")
    changes = entry.get("changes", [])

    changes_text = "\n".join(f"• {change}" for change in changes)
    changes_slack = changes_text

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    RELEASE_NOTES_PATH.write_text(changes_text, encoding="utf-8")

    lines = [
        f"CHANGELOG_TITLE={escape(title)}",
        f"CHANGELOG_VERSION={escape(version)}",
        f"CHANGELOG_DATE={escape(date)}",
        f"CHANGELOG_CHANGES={escape(changes_text)}",
        f"CHANGELOG_CHANGES_SLACK={escape(changes_slack)}",
    ]
    OUTPUT_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
