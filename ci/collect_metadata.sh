#!/usr/bin/env bash
# Collect build metadata into ci/build_metadata.env for notifications and archiving.
set -euo pipefail

OUTPUT="${1:-ci/build_metadata.env}"
BRANCH="${BRANCH:-unknown}"

VERSION_NAME="$(grep versionName app/build.gradle.kts | head -1 | sed 's/.*"\(.*\)".*/\1/')"
VERSION_CODE="$(grep versionCode app/build.gradle.kts | head -1 | grep -o '[0-9]*')"
AGP_VERSION="$(grep '^agp =' gradle/libs.versions.toml | sed 's/.*"\(.*\)".*/\1/')"
KOTLIN_VERSION="$(grep '^kotlin =' gradle/libs.versions.toml | sed 's/.*"\(.*\)".*/\1/')"
GRADLE_VERSION="$(grep distributionUrl gradle/wrapper/gradle-wrapper.properties | sed 's/.*gradle-\(.*\)-bin.zip/\1/')"

GIT_COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo unknown)"
GIT_COMMIT_FULL="$(git rev-parse HEAD 2>/dev/null || echo unknown)"
GIT_AUTHOR="$(git log -1 --format='%an' 2>/dev/null || echo unknown)"
GIT_COMMIT_MESSAGE="$(git log -1 --format='%s' 2>/dev/null || echo unknown)"

JAVA_VERSION="not detected"
if command -v java >/dev/null 2>&1; then
    _java_out="$(java -version 2>&1 | head -1 || true)"
    if [ -n "$_java_out" ] && [[ "$_java_out" != *"Unable to locate"* ]] && [[ "$_java_out" != *"couldn't be completed"* ]]; then
        JAVA_VERSION="$(echo "$_java_out" | sed 's/"//g')"
    fi
fi

# Android Studio version (optional — may not exist on all agents)
ANDROID_STUDIO_VERSION="not detected"
if command -v studio >/dev/null 2>&1; then
    ANDROID_STUDIO_VERSION="$(studio --version 2>/dev/null | head -1 || echo not detected)"
elif [ -x "/Applications/Android Studio.app/Contents/MacOS/studio" ]; then
    ANDROID_STUDIO_VERSION="$("/Applications/Android Studio.app/Contents/MacOS/studio" --version 2>/dev/null | head -1 || echo not detected)"
fi

GITHUB_REPO="CSI-Hymns-Book/CSI-Hymns-Android-Native"
GITHUB_COMMIT_URL="https://github.com/${GITHUB_REPO}/commit/${GIT_COMMIT_FULL}"
PLAY_CONSOLE_URL="https://play.google.com/console/developers/app/com.reyzie.hymns"

# Escape \, ", and newlines for loadDotenv() in Jenkinsfile (KEY=value, no wrapping quotes).
escape() {
    local s=$1
    s=${s//\\/\\\\}
    s=${s//\"/\\\"}
    s=${s//$'\n'/\\n}
    printf '%s' "$s"
}

cat > "$OUTPUT" <<EOF
VERSION_NAME=$(escape "$VERSION_NAME")
VERSION_CODE=$(escape "$VERSION_CODE")
AGP_VERSION=$(escape "$AGP_VERSION")
KOTLIN_VERSION=$(escape "$KOTLIN_VERSION")
GRADLE_VERSION=$(escape "$GRADLE_VERSION")
GIT_COMMIT=$(escape "$GIT_COMMIT")
GIT_COMMIT_FULL=$(escape "$GIT_COMMIT_FULL")
GIT_AUTHOR=$(escape "$GIT_AUTHOR")
GIT_COMMIT_MESSAGE=$(escape "$GIT_COMMIT_MESSAGE")
JAVA_VERSION=$(escape "$JAVA_VERSION")
ANDROID_STUDIO_VERSION=$(escape "$ANDROID_STUDIO_VERSION")
GITHUB_COMMIT_URL=$(escape "$GITHUB_COMMIT_URL")
PLAY_CONSOLE_URL=$(escape "$PLAY_CONSOLE_URL")
BRANCH=$(escape "$BRANCH")
EOF

echo "Metadata written to $OUTPUT"
