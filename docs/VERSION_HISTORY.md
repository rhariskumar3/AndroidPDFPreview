# Version History

This document provides a quick overview of all AndroidPDFPreview releases.

## 📋 Current Version

**[1.0.8](./CHANGELOG.md#108---2025-08-01)** - Latest Release (August 1, 2025)
- Added robust PDF page loading with multi-layer validation and retry mechanism to handle NaN values and prevent ANRs.
- Introduced adaptive delay in PDFView for improved rendering based on document size.
- Removed unwanted comments from PagesLoader for improved code clarity.

## 📚 Version History

| Version   | Release Date | Type  | Key Features                                                   |
|-----------|--------------|-------|----------------------------------------------------------------|
| **1.0.8** | 2025-08-01   | Minor | Enhanced PDF loading, adaptive rendering delay, code cleanup   |
| **1.0.7** | 2025-07-28   | Minor | Enhanced validation, performance improvements, stability fixes |
| **1.0.6** | Previous     | Minor | Core PDF features, Jetpack Compose support, validation         |

## 🔄 Version Status

- ✅ **1.0.8** - Current (Recommended)
- ⚠️ **1.0.7** - Previous (Still supported)
- ⚠️ **1.0.6** - Older (Limited support)

## 📦 Installation

### Current Version (Recommended)
```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.0.8'
}
```

### Previous Version
```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.0.7'
}
```

## 🚀 Upgrade Guide

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

- **Total Releases**: 3 major versions
- **Latest Release**: August 1, 2025
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
