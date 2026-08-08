#!/usr/bin/env python3
"""Send a rich Slack notification with optional APK/AAB attachments."""

import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path


def require_env(name: str) -> str:
    value = os.environ.get(name)
    if not value:
        print(f"Missing required env var: {name}", file=sys.stderr)
        sys.exit(1)
    return value


def slack_api(token: str, method: str, payload: dict) -> dict:
    url = f"https://slack.com/api/{method}"
    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=data,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json; charset=utf-8",
        },
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=120) as response:
        body = json.loads(response.read().decode("utf-8"))
    if not body.get("ok"):
        raise RuntimeError(f"Slack API {method} failed: {body.get('error', body)}")
    return body


def upload_file(token: str, channel: str, file_path: Path, title: str) -> None:
    import subprocess

    result = subprocess.run(
        [
            "curl",
            "-sS",
            "-F",
            f"token={token}",
            "-F",
            f"channels={channel}",
            "-F",
            f"title={title}",
            "-F",
            f"file=@{file_path}",
            "https://slack.com/api/files.upload",
        ],
        capture_output=True,
        text=True,
        check=False,
    )
    body = json.loads(result.stdout or "{}")
    if not body.get("ok"):
        raise RuntimeError(f"Slack file upload failed for {file_path}: {body.get('error', body)}")


def build_blocks(status: str, deploy_status: str) -> list:
    version = os.environ.get("VERSION_NAME", "unknown")
    version_code = os.environ.get("VERSION_CODE", "unknown")
    branch = os.environ.get("BRANCH", "unknown")
    build_number = os.environ.get("BUILD_NUMBER", "unknown")
    changelog_title = os.environ.get("CHANGELOG_TITLE", "")
    changelog_date = os.environ.get("CHANGELOG_DATE", "")
    changelog_changes = os.environ.get("CHANGELOG_CHANGES", "").replace("\\n", "\n")
    git_author = os.environ.get("GIT_AUTHOR", "unknown")
    git_commit = os.environ.get("GIT_COMMIT", "unknown")
    git_message = os.environ.get("GIT_COMMIT_MESSAGE", "")
    build_url = os.environ.get("BUILD_URL", "")
    github_url = os.environ.get("GITHUB_COMMIT_URL", "")
    play_url = os.environ.get("PLAY_CONSOLE_URL", "")

    emoji = "✅" if status == "success" else "❌"
    color = "#2eb886" if status == "success" else "#e01e5a"

    header = f"{emoji} CSI Hymns Android — {status.upper()}"

    if deploy_status == "deployed":
        deploy_line = f"🚀 Deployed to Google Play ({os.environ.get('PLAY_TRACK_LABEL', 'open')})"
    elif deploy_status == "skipped":
        deploy_line = "📦 Build only — Play upload skipped"
    elif deploy_status == "failed":
        deploy_line = "⚠️ Play upload failed"
    else:
        deploy_line = f"Deploy: {deploy_status}"

    blocks = [
        {
            "type": "header",
            "text": {"type": "plain_text", "text": header, "emoji": True},
        },
        {
            "type": "section",
            "fields": [
                {"type": "mrkdwn", "text": f"*Version:*\n{version} ({version_code})"},
                {"type": "mrkdwn", "text": f"*Branch:*\n{branch}"},
                {"type": "mrkdwn", "text": f"*Build:*\n#{build_number}"},
                {"type": "mrkdwn", "text": f"*Author:*\n{git_author}"},
            ],
        },
        {
            "type": "section",
            "text": {
                "type": "mrkdwn",
                "text": f"*Commit:* `{git_commit}` — {git_message}\n{deploy_line}",
            },
        },
    ]

    if changelog_title:
        changelog_text = f"*Changelog — {changelog_title}* ({changelog_date})\n{changelog_changes}"
        if len(changelog_text) > 2900:
            changelog_text = changelog_text[:2900] + "…"
        blocks.append(
            {
                "type": "section",
                "text": {"type": "mrkdwn", "text": changelog_text},
            }
        )

    links = []
    if build_url:
        links.append(f"<{build_url}|Jenkins Build>")
    if github_url:
        links.append(f"<{github_url}|GitHub Commit>")
    if play_url:
        links.append(f"<{play_url}|Play Console>")

    if links:
        blocks.append(
            {
                "type": "context",
                "elements": [{"type": "mrkdwn", "text": " · ".join(links)}],
            }
        )

    return blocks, color


def main() -> None:
    token = require_env("SLACK_BOT_TOKEN")
    channel = require_env("SLACK_CHANNEL")
    status = os.environ.get("BUILD_STATUS", "unknown")
    deploy_status = os.environ.get("DEPLOY_STATUS", "skipped")

    blocks, color = build_blocks(status, deploy_status)

    slack_api(
        token,
        "chat.postMessage",
        {
            "channel": channel,
            "text": f"CSI Hymns Android build {status}",
            "blocks": blocks,
            "attachments": [{"color": color, "blocks": []}],
        },
    )

    if status != "success":
        return

    apk_path = os.environ.get("APK_PATH", "")
    aab_path = os.environ.get("AAB_PATH", "")

    if apk_path and Path(apk_path).is_file():
        upload_file(token, channel, Path(apk_path), f"CSI-Hymns-{os.environ.get('VERSION_NAME', 'release')}.apk")

    if aab_path and Path(aab_path).is_file():
        upload_file(token, channel, Path(aab_path), f"CSI-Hymns-{os.environ.get('VERSION_NAME', 'release')}.aab")


if __name__ == "__main__":
    try:
        main()
    except (urllib.error.URLError, RuntimeError, json.JSONDecodeError) as exc:
        print(f"Slack notification failed: {exc}", file=sys.stderr)
        sys.exit(1)
