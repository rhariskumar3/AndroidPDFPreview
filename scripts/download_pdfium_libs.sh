#!/usr/bin/env bash

# Script to download PDFium .so files from GitHub releases for Android architectures

set -euo pipefail

echo "Starting PDFium library download script..."

# Directories
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"
echo "Current directory: $(pwd)"

# Release & base URL
RELEASE="chromium/7442"
BASE_URL="https://github.com/bblanchon/pdfium-binaries/releases/download/${RELEASE}"
echo "Release: ${RELEASE}"

# Target directory (absolute)
TARGET_DIR="$PROJECT_ROOT/pdfium/src/main/cpp/pdfium/lib"
echo "Target directory: $TARGET_DIR"

# Arch -> ABI mapping (portable)
ARCHS="arm:armeabi-v7a arm64:arm64-v8a x86:x86 x64:x86_64"

# Ensure target ABI dirs exist
for entry in $ARCHS; do
  abi="${entry##*:}"
  mkdir -p "$TARGET_DIR/$abi"
done

# Cleanup trap for leftover temp files if script exits
trap 'rm -rf "$PROJECT_ROOT"/temp_extract_* "$PROJECT_ROOT"/pdfium-android-*.tgz >/dev/null 2>&1 || true' EXIT

for entry in $ARCHS; do
  arch="${entry%%:*}"
  abi="${entry##*:}"
  filename="pdfium-android-${arch}.tgz"
  url="${BASE_URL}/${filename}"

  echo ""
  echo "▶ Processing arch=$arch → abi=$abi"
  echo "Downloading: $url"

  if ! curl -fL -o "$filename" "$url"; then
    echo "❌ Failed to download $url"
    exit 1
  fi

  temp_dir="$PROJECT_ROOT/temp_extract_${arch}"
  rm -rf "$temp_dir"
  mkdir -p "$temp_dir"
  pushd "$temp_dir" >/dev/null

  echo "Extracting $filename..."
  tar -xzf "$PROJECT_ROOT/$filename"

  so_file=$(find . -name "libpdfium.so" -type f | head -1 || true)
  if [ -z "$so_file" ]; then
    echo "❌ libpdfium.so not found inside $filename"
    popd >/dev/null
    exit 1
  fi

  # Copy libpdfium.so to target abi dir
  cp "$so_file" "$TARGET_DIR/$abi/"
  echo "✅ Copied libpdfium.so -> $TARGET_DIR/$abi/"

  # Show file size (portable)
  if [ -f "$TARGET_DIR/$abi/libpdfium.so" ]; then
    size=$(wc -c < "$TARGET_DIR/$abi/libpdfium.so" | tr -d ' ')
    echo "File size: ${size} bytes"
  fi

  # Optional architecture check using `file` if available
  if command -v file >/dev/null 2>&1; then
    file_output=$(file "$TARGET_DIR/$abi/libpdfium.so")
    echo "File type: $file_output"
    case "$abi" in
      "armeabi-v7a")
        if echo "$file_output" | grep -q "ARM"; then echo "✓ Architecture appears ARM (armeabi-v7a)"; else echo "⚠️ Architecture mismatch for $abi"; fi
        ;;
      "arm64-v8a")
        if echo "$file_output" | grep -q -E "AArch64|ARM aarch64"; then echo "✓ Architecture appears AArch64 (arm64-v8a)"; else echo "⚠️ Architecture mismatch for $abi"; fi
        ;;
      "x86")
        if echo "$file_output" | grep -q "80386"; then echo "✓ Architecture appears x86"; else echo "⚠️ Architecture mismatch for $abi"; fi
        ;;
      "x86_64")
        if echo "$file_output" | grep -q -E "x86-64|x86_64"; then echo "✓ Architecture appears x86_64"; else echo "⚠️ Architecture mismatch for $abi"; fi
        ;;
    esac
  else
    echo "ℹ️ 'file' command not found; skipping architecture verification"
  fi

  # For arm64: always copy include and licenses from archive, overwriting destination
  if [ "$arch" = "arm64" ]; then
    echo "→ arm64: copying include & licenses (will overwrite if exist)"
    for extra in include licenses; do
      if [ -d "$extra" ]; then
        dest="$PROJECT_ROOT/pdfium/src/main/cpp/pdfium/$extra"
        if [ -d "$dest" ]; then
          echo "Overwriting existing $dest"
          rm -rf "$dest"
        fi
        cp -r "$extra" "$PROJECT_ROOT/pdfium/src/main/cpp/pdfium/"
        echo "✅ Copied $extra -> $PROJECT_ROOT/pdfium/src/main/cpp/pdfium/"
      else
        echo "⚠️ $extra not found inside arm64 archive (skipping copy for this folder)"
      fi
    done
  fi

  popd >/dev/null

  # Cleanup this arch's temp and archive
  rm -rf "$temp_dir"
  rm -f "$PROJECT_ROOT/$filename"

  echo "✓ Completed: $arch -> $abi"
done

echo ""
echo "🎉 All PDFium binaries downloaded and copied successfully."