#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
: "${JAVA_HOME:?Set JAVA_HOME to a JDK}"
cargo build --locked --release --manifest-path "$root/native/misaki-bridge/Cargo.toml"
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT
"$JAVA_HOME/bin/javac" -d "$test_dir" "$root/native/misaki-bridge/tests/host-jni/org/example/paradeetts/MisakiBridge.java"
"$JAVA_HOME/bin/java" -Djava.library.path="$root/native/misaki-bridge/target/release" -cp "$test_dir" org.example.paradeetts.MisakiBridge
