# Version History

This document provides a quick overview of all AndroidPDFPreview releases.

## 📋 Current Version

**[1.1.1](./CHANGELOG.md#111---2025-09-04)** - Latest Release (January 04, 2025)

- **Performance and Stability Improvements**: Enhanced loading state management and lifecycle
  handling
- Prevented redundant PDF loads during recompositions in sample app
- Improved error handling and cancellation logic for robust PDF operations
- Better resource cleanup and state consistency throughout view lifecycle
- Enhanced logging and debugging capabilities for easier troubleshooting

## 📚 Version History

| Version   | Release Date | Type  | Key Features                                                                       |
|-----------|--------------|-------|------------------------------------------------------------------------------------|
| **1.1.1** | 2025-09-04   | Patch | Performance optimizations, loading state improvements, lifecycle enhancements      |
| **1.1.0** | 2025-09-03   | Major | Architecture refactor, separated configuration, DSL APIs, improved Compose support |
| **1.0.9** | 2025-08-21   | Minor | NDK optimization, async metadata, background processing, PDFium 141.0.7363.0       |
| **1.0.8** | 2025-08-01   | Minor | Enhanced PDF loading, adaptive rendering delay, code cleanup                       |
| **1.0.7** | 2025-07-28   | Minor | Enhanced validation, performance improvements, stability fixes                     |
| **1.0.6** | Previous     | Minor | Core PDF features, Jetpack Compose support, validation                             |

## 🔄 Version Status

- ✅ **1.1.1** - Current (Recommended)
- ✅ **1.1.0** - Previous (Still supported)
- ✅ **1.0.9** - Previous (Still supported)
- ⚠️ **1.0.8** - Previous (Limited support)
- ⚠️ **1.0.7** - Previous (Limited support)
- ❌ **1.0.6** - Older (Deprecated)

## 📦 Installation

### Current Version (Recommended)

```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.1.1'
}
```

### Previous Version

```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.1.0'
}
```

```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.0.8'
}
```

## 🚀 Upgrade Guide

### From 1.1.0 to 1.1.1

- **Compatibility**: ✅ Fully backward compatible
- **Breaking Changes**: None
- **Action Required**: None - just update the version number
- **Benefits**: Performance improvements, better loading state management, enhanced error handling

```gradle
// Old
implementation 'io.github.rhariskumar3:pdfpreview:1.1.0'

// New
implementation 'io.github.rhariskumar3:pdfpreview:1.1.1'
```

### From 1.0.9 to 1.1.0

- **Compatibility**: ✅ Fully backward compatible (deprecated APIs still work)
- **Breaking Changes**: None (old `enqueue()` method still functional)
- **Action Required**: Optional - migrate to new DSL APIs for better experience
- **Benefits**: Separated configuration, easier password retry, better Compose integration, improved
  performance

**Migration (Optional but Recommended):**

```kotlin
// Old API (still works)
pdfView.load(file) {
    swipeHorizontal(true)
    password("password")
}

// New API (recommended)
pdfView.configureView {
    swipeHorizontal(true)
}
pdfView.loadDocument(file) {
    password("password")
}
```

### From 1.0.8 to 1.0.9

- **Compatibility**: ✅ Fully backward compatible
- **Breaking Changes**: None
- **Action Required**: None - just update the version number
- **Benefits**: Optimized APK size, improved UI responsiveness, background processing, updated
  PDFium

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

- **Total Releases**: 5 versions
- **Latest Release**: September 04, 2025
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
