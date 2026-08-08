# CSI Hymns Android — CI scripts

Helper scripts used by `Jenkinsfile`. Jenkins injects signing credentials and notification tokens at runtime.

## Scripts

| Script | Purpose |
|--------|---------|
| `collect_metadata.sh` | Version, git info, Gradle/AGP/Kotlin versions → `build_metadata.env` |
| `read_changelog.py` | Latest `changelog.json` entry → `changelog.env` + `changelog_release.txt` |
| `notify_slack.py` | Rich Slack message + APK/AAB uploads |
| `notify_telegram.py` | Compact Telegram message + APK/AAB documents |

## Jenkins credentials (IDs)

| ID | Type |
|----|------|
| `android-keystore` | Secret file (.jks) |
| `android-keystore-password` | Secret text |
| `android-key-alias` | Secret text |
| `android-key-password` | Secret text |
| `google-play-service-account` | Secret file (JSON) |
| `slack-bot-token` | Secret text |
| `slack-build-channel` | Secret text (channel ID) |
| `telegram-bot-token` | Secret text |
| `telegram-chat-id` | Secret text |

## Google Play tracks

| Parameter | Fastlane track |
|-----------|----------------|
| `open` | `beta` (Open Testing, `release_status=completed`) |
| `production` | `production` draft (requires `CONFIRM_PRODUCTION=true`; promote manually in Play Console) |

## Fastlane / Ruby setup

macOS ships Ruby 2.6 at `/usr/bin/bundle`, which breaks native gem builds on modern macOS.
Use **Homebrew Ruby** and its Bundler:

```bash
brew install ruby          # if needed
gem install bundler
bash ci/setup_bundler.sh   # installs gems into vendor/bundle
```

Or manually:

```bash
export PATH="/opt/homebrew/opt/ruby/bin:/opt/homebrew/lib/ruby/gems/4.0.0/bin:$PATH"
bundle config set --local path 'vendor/bundle'
bundle install
```

Verify you're not using system bundler:

```bash
which bundle   # should NOT be /usr/bin/bundle
ruby --version # should be 4.x from Homebrew, not 2.6
```

Add to your shell `~/.zshrc` (optional):

```bash
export PATH="/opt/homebrew/opt/ruby/bin:/opt/homebrew/lib/ruby/gems/4.0.0/bin:$PATH"
```

## Local test

```bash
python3 ci/read_changelog.py
BRANCH=main bash ci/collect_metadata.sh ci/build_metadata.env
```
