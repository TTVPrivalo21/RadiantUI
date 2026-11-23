#!/usr/bin/env bash
# Builds RadiantUI: uses Maven when available, otherwise javac against ~/.m2 + jar into target/.
set -euo pipefail
cd "$(dirname "$0")"

NAME="RadiantUI"
VERSION="$(sed -n '0,/<version>/s/.*<version>\([^<]*\)<\/version>.*/\1/p' pom.xml | head -1)"
DESCRIPTION="$(sed -n 's/.*<description>\([^<]*\)<\/description>.*/\1/p' pom.xml | head -1)"
JAR_OUT="target/${NAME}-${VERSION}.jar"

if [ -x ./mvnw ]; then
  ./mvnw -q -DskipTests package
  echo "BUILD OK (mvnw) -> ${JAR_OUT}"; exit 0
fi
if command -v mvn >/dev/null 2>&1; then
  mvn -q -DskipTests package
  echo "BUILD OK (mvn) -> ${JAR_OUT}"; exit 0
fi

# ── javac fallback ──
M2="${HOME}/.m2/repository"
latest() { ls -d "$M2/$1"/*/ 2>/dev/null | sort -V | tail -1; }
pick() { local dir; dir="$(latest "$1")"; [ -n "$dir" ] && ls "$dir"*.jar 2>/dev/null | grep -v -E '(sources|javadoc)\.jar$' | head -1; }

DEPS=(
  "io/papermc/paper/paper-api"
  "net/kyori/adventure-api" "net/kyori/adventure-key" "net/kyori/adventure-text-minimessage"
  "net/kyori/adventure-text-serializer-gson" "net/kyori/adventure-text-serializer-json"
  "net/kyori/adventure-text-serializer-legacy" "net/kyori/adventure-text-serializer-plain"
  "net/kyori/adventure-text-serializer-commons" "net/kyori/adventure-text-logger-slf4j"
  "net/kyori/examination-api" "net/kyori/examination-string" "net/kyori/option"
  "com/google/code/gson/gson" "com/google/guava/guava" "org/jetbrains/annotations" "org/jspecify/jspecify"
  "net/md-5/bungeecord-chat" "org/joml/joml" "it/unimi/dsi/fastutil" "com/mojang/brigadier"
  "org/yaml/snakeyaml" "org/slf4j/slf4j-api" "org/apache/logging/log4j/log4j-api"
  "me/clip/placeholderapi"
)
CP=()
for dep in "${DEPS[@]}"; do
  jar="$(pick "$dep" || true)"
  if [ -n "${jar:-}" ]; then CP+=("$jar"); fi
done

SEP=":"; to_native() { printf '%s' "$1"; }
if command -v cygpath >/dev/null 2>&1; then SEP=";"; to_native() { cygpath -w "$1"; }; fi
CLASSPATH_STR=""
for j in "${CP[@]}"; do CLASSPATH_STR+="$(to_native "$j")${SEP}"; done

# Find a JDK "jar" tool (the Oracle javapath shim only exposes java/javac)
JAR_TOOL="$(command -v jar 2>/dev/null || true)"
if [ -z "$JAR_TOOL" ]; then
  for candidate in "${JAVA_HOME:-}/bin/jar" /c/Program\ Files/Java/*/bin/jar.exe /usr/lib/jvm/*/bin/jar; do
    if [ -x "$candidate" ]; then JAR_TOOL="$candidate"; fi
  done
fi
[ -n "$JAR_TOOL" ] || { echo "No se encontró la herramienta 'jar' del JDK"; exit 1; }

BUILD="target/local-build"
rm -rf "$BUILD" && mkdir -p "$BUILD/classes"
find src/main/java -name '*.java' > "$BUILD/sources.txt"
if command -v cygpath >/dev/null 2>&1; then
  while IFS= read -r f; do cygpath -w "$f"; done < "$BUILD/sources.txt" > "$BUILD/sources-native.txt"
  mv "$BUILD/sources-native.txt" "$BUILD/sources.txt"
fi

javac --release 21 -proc:none -encoding UTF-8 -Xlint:-deprecation -Xlint:-removal \
  -cp "$CLASSPATH_STR" -d "$BUILD/classes" @"$BUILD/sources.txt"

cp -r src/main/resources/. "$BUILD/classes/"
sed -i "s|\${project.version}|${VERSION}|g; s|\${project.description}|${DESCRIPTION}|g" "$BUILD/classes/plugin.yml"

mkdir -p target
rm -f "$JAR_OUT"
"$JAR_TOOL" --create --file "$JAR_OUT" -C "$BUILD/classes" .
echo "BUILD OK (javac) -> ${JAR_OUT} ($(du -k "$JAR_OUT" | cut -f1) KB)"
