#!/bin/bash

# Script to download PDFium .so files from GitHub releases for Android architectures

set -e

# Change to project root directory
cd "$(dirname "$0")/.."

echo "Starting PDFium library download script..."
echo "Current directory: $(pwd)"
echo "Release: chromium/7428"
echo "Target directory: pdfium/src/main/cpp/pdfium/lib"

# Latest release tag
RELEASE="chromium/7428"

# Base download URL
BASE_URL="https://github.com/bblanchon/pdfium-binaries/releases/download/${RELEASE}"

# Mapping of file suffix to Android ABI directory
declare -A ARCH_MAP=(
    ["arm"]="armeabi-v7a"
    ["arm64"]="arm64-v8a"
    ["x64"]="x86_64"
    ["x86"]="x86"
)

# Target directory
TARGET_DIR="pdfium/src/main/cpp/pdfium/lib"

echo "Creating target directories..."
# Create target directories if they don't exist
for arch in "${!ARCH_MAP[@]}"; do
    mkdir -p "${TARGET_DIR}/${ARCH_MAP[$arch]}"
    echo "Created directory: ${TARGET_DIR}/${ARCH_MAP[$arch]}"
done

echo "Downloading PDFium binaries for release: $RELEASE"

# Download and extract for each architecture
for arch in "${!ARCH_MAP[@]}"; do
    abi="${ARCH_MAP[$arch]}"
    filename="pdfium-android-${arch}.tgz"
    url="${BASE_URL}/${filename}"

    echo "Processing architecture: $abi"
    echo "Downloading $filename from $url..."

    # Download the archive
    curl -L -o "$filename" "$url"
    echo "Download completed: $filename"

    # Create temp directory for extraction
    temp_dir="temp_extract_${arch}"
    mkdir -p "$temp_dir"
    cd "$temp_dir"

    # Extract the archive
    echo "Extracting $filename..."
    tar -xzf "../$filename"
    echo "Extraction completed for $filename"
    echo "Extracted contents in $temp_dir:"
    ls -la
    find . -name "*.so" -type f

    # Find and copy the .so file
    so_file=$(find . -name "libpdfium.so" -type f | head -1)
    if [ -n "$so_file" ]; then
        cp "$so_file" "../${TARGET_DIR}/${abi}/"
        echo "Copied $so_file to ${TARGET_DIR}/${abi}/"
        echo "File size: $(stat -c%s "../${TARGET_DIR}/${abi}/libpdfium.so") bytes"

        # Verify the .so file
        echo "Verifying libpdfium.so for $abi..."
        if command -v file >/dev/null 2>&1; then
            file_output=$(file "../${TARGET_DIR}/${abi}/libpdfium.so")
            echo "File type: $file_output"
            case "$abi" in
                "armeabi-v7a")
                    if echo "$file_output" | grep -q "ARM"; then
                        echo "✓ Architecture verification passed for $abi"
                    else
                        echo "✗ Architecture verification failed for $abi"
                    fi
                    ;;
                "arm64-v8a")
                    if echo "$file_output" | grep -q "AArch64"; then
                        echo "✓ Architecture verification passed for $abi"
                    else
                        echo "✗ Architecture verification failed for $abi"
                    fi
                    ;;
                "x86")
                    if echo "$file_output" | grep -q "80386"; then
                        echo "✓ Architecture verification passed for $abi"
                    else
                        echo "✗ Architecture verification failed for $abi"
                    fi
                    ;;
                "x86_64")
                    if echo "$file_output" | grep -q "x86-64"; then
                        echo "✓ Architecture verification passed for $abi"
                    else
                        echo "✗ Architecture verification failed for $abi"
                    fi
                    ;;
            esac
        else
            echo "Warning: 'file' command not available, skipping architecture verification"
        fi

        # For arm64 architecture, also copy include and licenses folders
        if [ "$arch" = "arm64" ]; then
            echo "Copying include and licenses folders from arm64 architecture..."
            if [ -d "include" ]; then
                cp -r "include" "../pdfium/src/main/cpp/pdfium/"
                echo "Copied include folder"
            fi
            if [ -d "licenses" ]; then
                cp -r "licenses" "../pdfium/src/main/cpp/pdfium/"
                echo "Copied licenses folder"
            fi
        fi
    else
        echo "Error: libpdfium.so not found in extracted $filename"
        cd ..
        rm -rf "$temp_dir"
        rm "../$filename"
        exit 1
    fi

    # Clean up
    cd ..
    echo "Cleaning up temporary files..."
    rm -rf "$temp_dir"
    rm "$filename"
    echo "Cleanup completed for $filename"
done

echo "All PDFium binaries downloaded and copied successfully."
echo "Script completed."