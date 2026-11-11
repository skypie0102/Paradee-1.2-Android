#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to an Android NDK installation}"
toolchain="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$toolchain/aarch64-linux-android23-clang"
cargo build --locked --release --target aarch64-linux-android --manifest-path "$root/native/misaki-bridge/Cargo.toml"
mkdir -p "$root/app/src/main/jniLibs/arm64-v8a"
cp "$root/native/misaki-bridge/target/aarch64-linux-android/release/libparadee_misaki.so" "$root/app/src/main/jniLibs/arm64-v8a/"
