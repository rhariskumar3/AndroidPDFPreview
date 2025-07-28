# Changelog

All notable changes to AndroidPDFPreview will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.7] - 2025-07-28

### Changed
- **Internal API Refactoring**: Refactored `PDFView.kt` by changing visibility of numerous properties and methods from public to internal for better encapsulation
  - Properties now internal: `isRecycling`, `isLoading`, `isRecycled`, `isSwipeVertical`, `isDoubleTapEnabled`, `isBestQuality`, `isAnnotationRendering`, `isPageFlingEnabled`, `scrollHandle`, `renderingHandler`
  - Methods now internal: `onPageError`, `loadPages`, `redraw`, `onBitmapRendered`, `moveTo`, `isActivelyScrolling`, `setScrollHandleDragging`, `updateScrollUIElements`, `loadPageByOffset`, `performPageSnap`, `findFocusPage`, `pageFillsScreen`, `zoomTo`, `resetZoomWithAnimation`, `toCurrentScale`, `isZooming`, `callOnTap`, `callOnLongPress`, `callLinkHandler`
- Simplified the `enqueue` method by moving loading logic into a new private `startLoading` function

### Removed
- Unused properties: `lastLoadedSource`, `scrollDir`, `onDrawPagesNumbers`, `hasSize`
- `renderDuringScale` property (not effectively used)
- `toRealScale` method (unused)

### Fixed
- Minor cleanup in `recycle()` method
- Replaced unused exception catches with `(_: Exception)` pattern
- Added `isRecycled` status check at the beginning of `load()` method

### Migration Guide
No migration steps required. This is purely an internal refactoring release that maintains full backward compatibility for public API users.

```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.0.7'
}
```

## [1.0.6] - 2025-07-22

### Added
- **PDF Document Validation System**
  - `PDFDocumentValidator` utility class with comprehensive validation methods
  - `validateDocument()` method returning detailed `DocumentValidationResult`
  - Quick validation methods: `isDocumentValid()`, `isPasswordCorrect()`, `isPasswordProtected()`
  - `DocumentValidationResult` sealed class with validation outcomes (Valid, PasswordProtected, Corrupted, Invalid, Error)
  - Support for various document sources (File, Uri, ByteArray, InputStream, assets)
  - Background thread validation using coroutines

- **PDF Thumbnail Generation Feature**
  - `PDFThumbnailGenerator` with `generateThumbnail()` and `generateThumbnails()` suspend functions
  - `ThumbnailConfig` for custom configurations (width, height, quality, aspect ratio, background color)
  - `ThumbnailCache` with LRU memory-aware caching system
  - `AspectRatio` enum with options: PRESERVE, STRETCH, FIT_WIDTH, FIT_HEIGHT, CROP_TO_FIT
  - `getPageCount()` utility method

- **Enhanced Sample App UI**
  - `PDFPreviewState` sealed class for state management (NoPdfSelected, PdfSelected, PdfPreviewing)
  - PDF details card with thumbnail preview, file info, and page count
  - Improved navigation with back button support
  - Async thumbnail generation and file info retrieval

### Changed
- **Memory Management Improvements**
  - Fixed multiple memory leaks in PDFium JNI layer (`pdfium_jni.cpp`)
  - Fixed buffer leaks in `nativeOpenMemDocument`
  - Resolved FPDF_PAGE resource leaks in text operations
  - Fixed string buffer leaks in search and annotation operations
  - `PdfiumCore` now implements `java.io.Closeable` for better resource management
  - Added proper tracking and cleanup of search handles

- **Performance Enhancements**
  - Enhanced zoom handling and rendering quality
  - Implemented scroll optimization for improved performance during scrolling
  - Modernized DefaultScrollHandle UI with improved positioning logic

- **Platform Updates**
  - Updated PDFium to version 140.0.7309.0
  - Updated build for Android 15+ with 16KB page size support
  - Upgraded compileSdk to 35
  - Upgraded Java version to 17 in build configuration
  - Updated Gradle Maven Publish Plugin to 0.34.0
  - Simplified Sonatype host configuration

### Fixed
- Enhanced PDF loading and rendering stability by preventing concurrent operations
- Better handling of recycling state during PDF operations
- Improved resource management throughout the library

### Documentation
- Added comprehensive PDF validation guide with examples
- Added PDF thumbnail generation section with Kotlin and Jetpack Compose examples
- Updated feature list to include new validation and thumbnail capabilities
- Enhanced README with detailed usage examples

## [1.0.5] - Previous Release

### Added
- Core PDF display and interaction functionality
- Jetpack Compose migration and support
- Basic scroll handle improvements
- Cache management improvements

### Technical Details
- Built on PdfiumAndroid for PDF decoding
- AndroidPdfViewer for rendering engine
- Compatible with Android API 21+ (Android 5.1 and above)
- Kotlin support

---

## Support & Contributing

- **Documentation**: Check the [README.md](../README.md) for usage instructions
- **Issues**: Report bugs or request features on [GitHub Issues](https://github.com/rhariskumar3/AndroidPDFPreview/issues)
- **Discussions**: Join conversations on [GitHub Discussions](https://github.com/rhariskumar3/AndroidPDFPreview/discussions)

## Release Types

- **Added** for new features
- **Changed** for changes in existing functionality
- **Deprecated** for soon-to-be removed features
- **Removed** for now removed features
- **Fixed** for any bug fixes
- **Security** for security-related improvements

## Version Links

- [1.0.7](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.0.7) - Latest
- [1.0.6](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.0.6)
- [1.0.5](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.0.5)
