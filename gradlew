#!/usr/bin/env sh
# HSE AI PRO build launcher.
#
# This repository intentionally uses the pinned Gradle version supplied by
# GitHub Actions (8.7). The launcher does not depend on the executable bit, so
# `sh gradlew ...` also works when a ZIP upload strips Unix file permissions.
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$ROOT_DIR"

if [ ! -f settings.gradle ] || [ ! -f build.gradle ] || [ ! -d app ]; then
  echo "ERROR: HSE AI PRO project root was not detected." >&2
  echo "Expected settings.gradle, build.gradle and app/ beside gradlew." >&2
  echo "Current directory: $ROOT_DIR" >&2
  exit 2
fi

if ! command -v gradle >/dev/null 2>&1; then
  echo "ERROR: Gradle 8.7 is not available on PATH." >&2
  echo "GitHub Actions installs the pinned Gradle 8.7 toolchain before this launcher runs." >&2
  exit 127
fi

exec gradle "$@"
