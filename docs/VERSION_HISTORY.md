# Version History

This document provides a quick overview of all AndroidPDFPreview releases.

## 📋 Current Version

**[1.2.7](./CHANGELOG.md#127---2025-11-05---ui-enhancements--performance-optimizations)** - Latest
Release (November 5, 2025)

- Modern DocumentDetailsCard UI redesign with card-based layout
- Ultra-fast configuration to eliminate white pages during scrolling
- Self-documenting API with descriptive parameter names
- 30% smoother scrolling and 95% reduction in white page occurrences

## 📚 Version History

| Version   | Release Date | Type  | Key Features                                                                       |
|-----------|--------------|-------|------------------------------------------------------------------------------------|
| **1.2.7** | 2025-11-05   | Minor | UI enhancements, ultra-fast scrolling, self-documenting API                        |
| **1.2.6** | 2025-11-03   | Patch | Critical bug fix: single-page mode navigation & PDFium chromium/7506 upgrade       |
| **1.2.5** | 2025-10-29   | Patch | Critical bug fix: page navigation callbacks & page snap behavior                   |
| **1.2.4** | 2025-10-28   | Patch | Critical bug fix: page calculation accuracy with page snap enabled                 |
| **1.2.3** | 2025-10-28   | Patch | Critical bug fix: page navigation callbacks now fire correctly after manual scroll |
| **1.2.2** | 2025-10-24   | Minor | Performance optimization: smart tile loading, scroll pre-rendering, zoom listener  |
| **1.2.1** | 2025-10-03   | Minor | Fix password retry issue & update PDFium to chromium/7442                          |
| **1.2.0** | 2025-09-24   | Minor | Single page mode, PDFium chromium/7428 update, API cleanup, documentation updates  |
| **1.1.1** | 2025-09-04   | Patch | Performance optimizations, loading state improvements, lifecycle enhancements      |
| **1.1.0** | 2025-09-03   | Major | Architecture refactor, separated configuration, DSL APIs, improved Compose support |
| **1.0.9** | 2025-08-21   | Minor | NDK optimization, async metadata, background processing, PDFium 141.0.7363.0       |
| **1.0.8** | 2025-08-01   | Minor | Enhanced PDF loading, adaptive rendering delay, code cleanup                       |
| **1.0.7** | 2025-07-28   | Minor | Enhanced validation, performance improvements, stability fixes                     |
| **1.0.6** | Previous     | Minor | Core PDF features, Jetpack Compose support, validation                             |

## 🔄 Version Status

- ✅ **1.2.7** - Current (Recommended)
- ✅ **1.2.6** - Previous (Still supported)
- ✅ **1.2.5** - Previous (Still supported)
- ✅ **1.2.4** - Previous (Still supported)
- ✅ **1.2.3** - Previous (Still supported)
- ✅ **1.2.2** - Previous (Still supported)
- ✅ **1.2.1** - Previous (Still supported)
- ✅ **1.2.0** - Previous (Still supported)
- ✅ **1.1.1** - Previous (Still supported)
- ✅ **1.1.0** - Previous (Still supported)
- ❌ **1.0.9** - Older (Deprecated)

## 📦 Installation

### Current Version (Recommended)

```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.2.7'
}
```

### Previous Version

```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.2.5'
}
```

```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.0.8'
}
```

## 🚀 Upgrade Guide

### From 1.2.6 to 1.2.7

- **Compatibility**: ✅ Fully backward compatible
- **Breaking Changes**: None
- **Action Required**: None - just update the version number
- **Benefits**:
    - Modern DocumentDetailsCard UI with improved visual hierarchy
    - Ultra-fast scrolling configuration eliminates white pages during scrolling
    - Self-documenting API with descriptive parameter names for better developer experience
    - 30% smoother scrolling performance and 95% reduction in white page occurrences

```gradle
// Old
implementation 'io.github.rhariskumar3:pdfpreview:1.2.6'

// New
implementation 'io.github.rhariskumar3:pdfpreview:1.2.7'
```

#### Optional: Enable Ultra-Fast Scrolling

```kotlin
pdfView.configureView {
    // Enable ultra-fast scrolling to eliminate white pages
    ultraFastConfiguration(true)
}
```

#### Optional: Use New Descriptive Parameter Names

```kotlin
// NEW (recommended - more descriptive)
pdfView.configureView {
    enableHorizontalSwipe(true)
    enablePageSnap(true)
    enableDoubleTapZoom(true)
    onZoomChanged { newZoom, oldZoom -> }
}

// OLD (still works - backward compatible)
pdfView.configureView {
    swipeHorizontal(true)
    pageSnap(true)
    doubleTapZoom(true)
    zoomEventListener { newZoom, oldZoom -> }
}
```

### From 1.2.5 to 1.2.6

- **Compatibility**: ✅ Fully backward compatible
- **Breaking Changes**: None
- **Action Required**: None - just update the version number
- **Benefits**:
    - Fixed `jumpTo()` automatically loading last page in single-page mode
    - Fixed duplicate `onPageChanged()` callbacks in single-page mode
    - Upgraded PDFium to chromium/7506 with latest improvements and bug fixes
    - Enhanced single-page mode navigation reliability

```gradle
// Old
implementation 'io.github.rhariskumar3:pdfpreview:1.2.5'

// New
implementation 'io.github.rhariskumar3:pdfpreview:1.2.6'
```

### From 1.2.4 to 1.2.5

- **Compatibility**: ✅ Fully backward compatible
- **Breaking Changes**: None
- **Action Required**: None - just update the version number
- **Benefits**:
    - `onPageChanged()` callback now fires after scroll/animation completion
    - `onPageScrolled()` no longer reports intermediate pages during page snap animations
    - Pages properly center with page snap enabled during first load
    - Improved page navigation callback reliability across all scenarios

```gradle
// Old
implementation 'io.github.rhariskumar3:pdfpreview:1.2.4'

// New
implementation 'io.github.rhariskumar3:pdfpreview:1.2.5'
```

### From 1.2.3 to 1.2.4

- **Compatibility**: ✅ Fully backward compatible
- **Breaking Changes**: None
- **Action Required**: None - just update the version number
- **Benefits**:
    - `onPageScrolled()` reports accurate page numbers during page snap animations
    - No more page number jumping with `pageSnap=true` configuration
    - Consistent page detection across all scroll scenarios
    - Improved callback reliability during scroll deceleration

```gradle
// Old
implementation 'io.github.rhariskumar3:pdfpreview:1.2.3'

// New
implementation 'io.github.rhariskumar3:pdfpreview:1.2.4'
```

### From 1.2.2 to 1.2.3

- **Compatibility**: ✅ Fully backward compatible
- **Breaking Changes**: None
- **Action Required**: None - just update the version number
- **Benefits**:
    - `onPageChanged()` callback now fires correctly after manual scroll stops
    - `onPageScrolled()` reports accurate page numbers during scroll
    - More reliable page navigation callbacks across all scenarios
    - Reduced redundant page loading operations

```gradle
// Old
implementation 'io.github.rhariskumar3:pdfpreview:1.2.2'

// New
implementation 'io.github.rhariskumar3:pdfpreview:1.2.3'
```

### From 1.2.1 to 1.2.2

- **Compatibility**: ✅ Fully backward compatible
- **Breaking Changes**: None
- **Action Required**: None - just update the version number
- **Benefits**:
    - 75-80% faster perceived tile loading with center-first rendering
    - Hybrid scroll optimization eliminates post-scroll delay (2-3s → ~500ms)
    - New ZoomEventListener for tracking zoom changes
    - Reset zoom option in jumpTo() method
    - 90% reduction in cache thrashing at high zoom levels

```gradle
// Old
implementation 'io.github.rhariskumar3:pdfpreview:1.2.1'

// New
implementation 'io.github.rhariskumar3:pdfpreview:1.2.2'
```

**Optional - Add Zoom Listener:**

```kotlin
pdfView.configureView {
    zoomEventListener { newZoom, oldZoom ->
        // React to zoom changes
    }
}
```

### From 1.1.1 to 1.2.1

- **Compatibility**: ✅ Fully backward compatible
- **Breaking Changes**: None
- **Action Required**: None - just update the version number
- **Benefits**: Prevent premature resource recycling on incorrect password

```gradle
// Old
implementation 'io.github.rhariskumar3:pdfpreview:1.2.0'

// New
implementation 'io.github.rhariskumar3:pdfpreview:1.2.1'
```

### From 1.1.1 to 1.2.0

- **Compatibility**: ✅ Fully backward compatible
- **Breaking Changes**: None
- **Action Required**: None - just update the version number
- **Benefits**: Single page mode support, latest PDFium features, cleaner API, improved
  documentation

```gradle
// Old
implementation 'io.github.rhariskumar3:pdfpreview:1.1.1'

// New
implementation 'io.github.rhariskumar3:pdfpreview:1.2.0'
```

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

- **Total Releases**: 12 versions
- **Latest Release**: November 5, 2025
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
