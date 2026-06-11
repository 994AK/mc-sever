#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PROJECT_DIR="$ROOT_DIR/src/LeafGomoku"
BUILD_DIR="$PROJECT_DIR/build-test"
MAIN_CLASSES="$BUILD_DIR/main"
TEST_CLASSES="$BUILD_DIR/test"
PAPER_API="$ROOT_DIR/cache/paper-api-1.21.11-R0.1-SNAPSHOT.jar"
DEPS_DIR="$ROOT_DIR/cache/leafgomoku-deps"
REPO_URL="https://repo.papermc.io/repository/maven-public"
MAVEN_CENTRAL_URL="https://repo1.maven.org/maven2"
SQLITE_JDBC_VERSION="3.53.2.0"
SLF4J_API_VERSION="2.0.18"

download_dep_from() {
  local repo_url="$1"
  local group="$2"
  local artifact="$3"
  local version="$4"
  local group_path
  group_path="$(printf '%s' "$group" | tr '.' '/')"
  local jar_path="$DEPS_DIR/$artifact-$version.jar"
  local url="$repo_url/$group_path/$artifact/$version/$artifact-$version.jar"
  if [[ ! -f "$jar_path" ]]; then
    mkdir -p "$DEPS_DIR"
    curl -fL -o "$jar_path" "$url"
  fi
}

download_dep() {
  download_dep_from "$REPO_URL" "$1" "$2" "$3"
}

download_central_dep() {
  download_dep_from "$MAVEN_CENTRAL_URL" "$1" "$2" "$3"
}

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
download_central_dep org.xerial sqlite-jdbc "$SQLITE_JDBC_VERSION"
download_central_dep org.slf4j slf4j-api "$SLF4J_API_VERSION"

CLASSPATH="$PAPER_API"
for dep in "$DEPS_DIR"/*.jar; do
  CLASSPATH="$CLASSPATH:$dep"
done
if [[ -f "$ROOT_DIR/plugins/PlaceholderAPI-2.12.2.jar" ]]; then
  CLASSPATH="$CLASSPATH:$ROOT_DIR/plugins/PlaceholderAPI-2.12.2.jar"
fi

rm -rf "$BUILD_DIR"
mkdir -p "$MAIN_CLASSES" "$TEST_CLASSES"

javac \
  --release 21 \
  -encoding UTF-8 \
  -cp "$CLASSPATH" \
  -d "$MAIN_CLASSES" \
  $(find "$PROJECT_DIR/src/main/java" -name '*.java' | sort)

javac \
  --release 21 \
  -encoding UTF-8 \
  -cp "$CLASSPATH:$MAIN_CLASSES" \
  -d "$TEST_CLASSES" \
  $(find "$PROJECT_DIR/src/test/java" -name '*.java' | sort)

for test_class in \
  net.leafmc.gomoku.GomokuRulesTest \
  net.leafmc.gomoku.MatchControllerTest \
  net.leafmc.gomoku.BoardGeometryTest \
  net.leafmc.gomoku.RoomLayoutFactoryTest \
  net.leafmc.gomoku.AppearanceCatalogTest \
  net.leafmc.gomoku.SqliteDataStoreTest \
  net.leafmc.gomoku.AppearanceUnlockServiceTest \
  net.leafmc.gomoku.StatsServiceTest
do
  java -cp "$CLASSPATH:$MAIN_CLASSES:$TEST_CLASSES" "$test_class"
done

echo "LeafGomoku tests passed"
