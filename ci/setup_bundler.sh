#!/usr/bin/env bash
# Use Homebrew Ruby + Bundler (not macOS system Ruby 2.6 at /usr/bin/bundle).
set -euo pipefail

export PATH="/opt/homebrew/opt/ruby/bin:/opt/homebrew/lib/ruby/gems/4.0.0/bin:${PATH}"

if ! command -v ruby >/dev/null 2>&1; then
    echo "Ruby not found. Install with: brew install ruby" >&2
    exit 1
fi

if ! command -v bundle >/dev/null 2>&1; then
    echo "Bundler not found. Install with: gem install bundler" >&2
    exit 1
fi

echo "Ruby: $(ruby --version)"
echo "Bundler: $(bundle --version)"
echo "bundle path: $(command -v bundle)"

bundle config set --local path 'vendor/bundle'
bundle check || bundle install
