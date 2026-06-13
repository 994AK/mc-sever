#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
exec "$ROOT_DIR/gradlew" --project-dir "$ROOT_DIR" :LeafResidenceWeb:installPlugin
