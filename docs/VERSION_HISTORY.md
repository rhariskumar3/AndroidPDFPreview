# Version History

This document provides a quick overview of all AndroidPDFPreview releases.

## 📋 Current Version

**[1.0.9](./CHANGELOG.md#109---2025-08-21)** - Latest Release (August 21, 2025)
- Added NDK ABI filters for optimized APK size and better device compatibility.
- Made PDF metadata and bookmark retrieval asynchronous for improved UI responsiveness.
- Enhanced background processing for PDF loading in sample app.
- Updated PDFium libraries to version 141.0.7363.0.
- Simplified PDF loading logic and improved performance.

## 📚 Version History

| Version   | Release Date | Type  | Key Features                                                                 |
|-----------|--------------|-------|------------------------------------------------------------------------------|
| **1.0.9** | 2025-08-21   | Minor | NDK optimization, async metadata, background processing, PDFium 141.0.7363.0 |
| **1.0.8** | 2025-08-01   | Minor | Enhanced PDF loading, adaptive rendering delay, code cleanup                 |
| **1.0.7** | 2025-07-28   | Minor | Enhanced validation, performance improvements, stability fixes               |
| **1.0.6** | Previous     | Minor | Core PDF features, Jetpack Compose support, validation                       |

## 🔄 Version Status

- ✅ **1.0.9** - Current (Recommended)
- ⚠️ **1.0.8** - Previous (Still supported)
- ⚠️ **1.0.7** - Previous (Limited support)
- ❌ **1.0.6** - Older (Deprecated)

## 📦 Installation

### Current Version (Recommended)
```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.0.9'
}
```

### Previous Version
```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.0.8'
}
```

## 🚀 Upgrade Guide

### From 1.0.8 to 1.0.9
- **Compatibility**: ✅ Fully backward compatible
- **Breaking Changes**: None
- **Action Required**: None - just update the version number
- **Benefits**: Optimized APK size, improved UI responsiveness, background processing, updated PDFium

```gradle
// Old
implementation 'io.github.rhariskumar3:pdfpreview:1.0.8'

// New
implementation 'io.github.rhariskumar3:pdfpreview:1.0.9'
```

### From 1.0.7 to 1.0.8
- **Compatibility**: ✅ Fully backward compatible
- **Breaking Changes**: None
- **Action Required**: None - just update the version number
- **Benefits**: Improved PDF loading, adaptive rendering, code clarity

```gradle
// Old
implementation 'io.github.rhariskumar3:pdfpreview:1.0.7'

// New
implementation 'io.github.rhariskumar3:pdfpreview:1.0.8'
```

## 📊 Release Statistics

- **Total Releases**: 4 major versions
- **Latest Release**: August 21, 2025
- **Release Frequency**: Regular updates with improvements
- **Stability**: Stable and production-ready

## 🔍 Version Details

For detailed information about each version:
- **[Changelog](./CHANGELOG.md)** - Complete changelog following standard format
- **[Version History](./VERSION_HISTORY.md)** - Quick overview and upgrade guides  
- **[README](../README.md)** - Usage documentation and examples

## 🎯 Future Roadmap

Upcoming features being considered:
- Advanced search functionality
- Enhanced annotation support
- Performance optimizations
- Additional Jetpack Compose components

## 💬 Feedback

Have suggestions for future versions? 
- Open an [issue](https://github.com/rhariskumar3/AndroidPDFPreview/issues)
- Start a [discussion](https://github.com/rhariskumar3/AndroidPDFPreview/discussions)

## Deprecated Versions

### **1.0.7 - [DEPRECATED]**
- Major bug present. DO NOT USE this version.

### **1.0.6 - [DEPRECATED]**
- Older version with limited support. Upgrade to latest version recommended.
