#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PROJECT_DIR="$ROOT_DIR/src/LeafFriends"
BUILD_DIR="$PROJECT_DIR/build"
CLASSES_DIR="$BUILD_DIR/classes"
PAPER_API="$ROOT_DIR/cache/paper-api-1.21.11-R0.1-SNAPSHOT.jar"
PAPER_API_URL="https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/1.21.11-R0.1-SNAPSHOT/paper-api-1.21.11-R0.1-20260511.115010-91.jar"
DEPS_DIR="$ROOT_DIR/cache/leaffriends-deps"
OUTPUT_JAR="$ROOT_DIR/plugins/LeafFriends-0.1.0.jar"
REPO_URL="https://repo.papermc.io/repository/maven-public"

download_dep() {
  local group="$1"
  local artifact="$2"
  local version="$3"
  local group_path
  group_path="$(printf '%s' "$group" | tr '.' '/')"
  local jar_path="$DEPS_DIR/$artifact-$version.jar"
  local url="$REPO_URL/$group_path/$artifact/$version/$artifact-$version.jar"
  if [[ ! -f "$jar_path" ]]; then
    mkdir -p "$DEPS_DIR"
    curl -fL -o "$jar_path" "$url"
  fi
}

if [[ ! -f "$PAPER_API" ]]; then
  mkdir -p "$(dirname "$PAPER_API")"
  curl -fL -o "$PAPER_API" "$PAPER_API_URL"
fi

download_dep net.kyori adventure-api 4.26.1
download_dep net.kyori adventure-key 4.26.1
download_dep net.kyori adventure-text-minimessage 4.26.1
download_dep net.kyori adventure-text-serializer-gson 4.26.1
download_dep net.kyori adventure-text-serializer-json 4.26.1
download_dep net.kyori adventure-text-serializer-legacy 4.26.1
download_dep net.kyori adventure-text-serializer-plain 4.26.1
download_dep net.kyori adventure-text-logger-slf4j 4.26.1
download_dep net.kyori examination-api 1.3.0
download_dep net.kyori examination-string 1.3.0
download_dep org.jetbrains annotations 26.0.2-1
download_dep net.md-5 bungeecord-chat '1.21-R0.2-deprecated+build.21'
download_dep org.joml joml 1.10.8
download_dep com.google.guava guava 33.5.0-jre
download_dep org.yaml snakeyaml 2.5

CLASSPATH="$PAPER_API"
for dep in "$DEPS_DIR"/*.jar; do
  CLASSPATH="$CLASSPATH:$dep"
done

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR"

javac \
  --release 21 \
  -encoding UTF-8 \
  -cp "$CLASSPATH" \
  -d "$CLASSES_DIR" \
  $(find "$PROJECT_DIR/src/main/java" -name '*.java' | sort)

cp -R "$PROJECT_DIR/src/main/resources/." "$CLASSES_DIR/"
jar --create --file "$OUTPUT_JAR" --date "2026-06-11T00:00:00Z" -C "$CLASSES_DIR" .

echo "Built $OUTPUT_JAR"
