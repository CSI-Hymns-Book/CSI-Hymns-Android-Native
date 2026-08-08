#!/usr/bin/env python3
"""Send a compact Telegram notification with optional APK/AAB documents."""

import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

# Bot API sendDocument limit (bots): 50 MiB.
TELEGRAM_DOCUMENT_MAX_BYTES = 50 * 1024 * 1024


def require_env(name: str) -> str:
    value = os.environ.get(name)
    if not value:
        print(f"Missing required env var: {name}", file=sys.stderr)
        sys.exit(1)
    return value


def telegram_api(token: str, method: str, payload: dict | None = None) -> dict:
    url = f"https://api.telegram.org/bot{token}/{method}"
    data = None
    headers = {}
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"

    request = urllib.request.Request(url, data=data, headers=headers, method="POST")
    with urllib.request.urlopen(request, timeout=120) as response:
        body = json.loads(response.read().decode("utf-8"))
    if not body.get("ok"):
        raise RuntimeError(f"Telegram API {method} failed: {body.get('description', body)}")
    return body


def send_document(token: str, chat_id: str, file_path: Path, caption: str) -> None:
    import subprocess

    size = file_path.stat().st_size
    if size > TELEGRAM_DOCUMENT_MAX_BYTES:
        print(
            f"Skipping Telegram upload for {file_path.name}: "
            f"{size} bytes exceeds {TELEGRAM_DOCUMENT_MAX_BYTES}-byte Bot API limit",
            file=sys.stderr,
        )
        return

    result = subprocess.run(
        [
            "curl",
            "-sS",
            "-F",
            f"chat_id={chat_id}",
            "-F",
            f"document=@{file_path}",
            "-F",
            f"caption={caption}",
            f"https://api.telegram.org/bot{token}/sendDocument",
        ],
        capture_output=True,
        text=True,
        check=False,
    )
    body = json.loads(result.stdout or "{}")
    if not body.get("ok"):
        raise RuntimeError(f"Telegram document upload failed for {file_path}: {body.get('description', body)}")


def build_message(status: str, deploy_status: str) -> str:
    version = os.environ.get("VERSION_NAME", "unknown")
    version_code = os.environ.get("VERSION_CODE", "unknown")
    branch = os.environ.get("BRANCH", "unknown")
    build_number = os.environ.get("BUILD_NUMBER", "unknown")
    changelog_title = os.environ.get("CHANGELOG_TITLE", "")
    changelog_date = os.environ.get("CHANGELOG_DATE", "")
    changelog_changes = os.environ.get("CHANGELOG_CHANGES", "").replace("\\n", "\n")
    build_url = os.environ.get("BUILD_URL", "")
    github_url = os.environ.get("GITHUB_COMMIT_URL", "")

    emoji = "✅" if status == "success" else "❌"

    if deploy_status == "deployed":
        deploy_line = f"Deployed to Play ({os.environ.get('PLAY_TRACK_LABEL', 'open')})"
    elif deploy_status == "skipped":
        deploy_line = "Play upload skipped"
    elif deploy_status == "failed":
        deploy_line = "Play upload FAILED"
    else:
        deploy_line = deploy_status

    lines = [
        f"{emoji} CSI Hymns Android — {status.upper()}",
        f"Version: {version} ({version_code})",
        f"Branch: {branch} · Build #{build_number}",
        deploy_line,
    ]

    if changelog_title:
        lines.append(f"\n{changelog_title} ({changelog_date})")
        # Telegram messages cap at 4096 chars — keep changelog compact
        if len(changelog_changes) > 1200:
            changelog_changes = changelog_changes[:1200] + "…"
        lines.append(changelog_changes)

    if build_url:
        lines.append(f"\nJenkins: {build_url}")
    if github_url:
        lines.append(f"GitHub: {github_url}")

    return "\n".join(lines)


def main() -> None:
    token = require_env("TELEGRAM_BOT_TOKEN")
    chat_id = require_env("TELEGRAM_CHAT_ID")
    status = os.environ.get("BUILD_STATUS", "unknown")
    deploy_status = os.environ.get("DEPLOY_STATUS", "skipped")

    message = build_message(status, deploy_status)
    if len(message) > 4096:
        message = message[:4093] + "…"

    telegram_api(token, "sendMessage", {"chat_id": chat_id, "text": message})

    if status != "success":
        return

    version = os.environ.get("VERSION_NAME", "release")
    apk_path = os.environ.get("APK_PATH", "")
    aab_path = os.environ.get("AAB_PATH", "")

    if apk_path and Path(apk_path).is_file():
        send_document(token, chat_id, Path(apk_path), f"CSI Hymns APK {version}")

    if aab_path and Path(aab_path).is_file():
        send_document(token, chat_id, Path(aab_path), f"CSI Hymns AAB {version}")


if __name__ == "__main__":
    try:
        main()
    except (urllib.error.URLError, RuntimeError, json.JSONDecodeError, OSError) as exc:
        print(f"Telegram notification failed: {exc}", file=sys.stderr)
        sys.exit(1)
