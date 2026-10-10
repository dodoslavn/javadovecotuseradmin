#!/bin/bash
# Compiles the webapp with javac against apt-provided jars + vendored libs, then packages a WAR.
# No Maven/Gradle — apt-only (plus the one deliberately vendored sentry jar).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="$ROOT/src/main/java"
WEBAPP="$ROOT/src/main/webapp"
BUILD="$ROOT/build"
CLASSES="$BUILD/WEB-INF/classes"
WAR="$ROOT/javaDovecotAdmin.war"

rm -rf "$BUILD" "$WAR"
mkdir -p "$CLASSES"

CLASSPATH="/usr/share/java/jakarta-servlet-api.jar"
for jar in "$WEBAPP/WEB-INF/lib"/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done

find "$SRC" -name '*.java' > "$BUILD.sources.txt"
javac -d "$CLASSES" -cp "$CLASSPATH" -encoding UTF-8 @"$BUILD.sources.txt"
rm -f "$BUILD.sources.txt"

if [ -d "$ROOT/src/main/resources" ]; then
    cp -r "$ROOT/src/main/resources"/* "$CLASSES/"
fi

cp -r "$WEBAPP"/* "$BUILD/"

cd "$BUILD"
jar cf "$WAR" -C "$BUILD" .
echo "Built $WAR"
