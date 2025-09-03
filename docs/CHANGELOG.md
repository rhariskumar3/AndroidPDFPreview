# Changelog

All notable changes to AndroidPDFPreview will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2025-09-03 - Major Architecture Refactor

### Added

- **🏗️ New Separated Configuration Architecture**
    - `PdfViewConfiguration` class for factory-time settings (view behavior, rendering, listeners)
    - `PdfLoadRequest` class for runtime settings (document source, password, page selection)
    - `PDFView.configure(PdfViewConfiguration)` method for applying view configuration
    - `PDFView.load(PdfLoadRequest)` method for loading documents with runtime settings
    - Extension methods `PdfRequest.toViewConfiguration()` and `PdfRequest.toLoadRequest()` for
      migration

- **🔐 Enhanced Password Management**
    - Runtime password updates without losing view state
    - Easy password retry scenarios with `PdfLoadRequest.copy(password = newPassword)`
    - Support for document switching while preserving passwords

- **⚡ Performance Improvements**
    - Factory-time configuration cached and reused across document loads
    - Reduced object allocation for repeated document operations
    - Optimized memory usage with clear separation of concerns

- **🎯 Jetpack Compose Integration**
    - Natural support for AndroidView factory/update pattern
    - Proper recomposition handling with separated configuration
    - Document switching without view reconfiguration

### Changed

- **📋 API Evolution**
    - `PdfRequest` and `PDFView.enqueue()` method marked as deprecated (but fully supported)
    - Improved error handling with clearer exception paths
    - Better separation between factory-time and runtime concerns

### Deprecated

- `PdfRequest` class - Use `PdfViewConfiguration` + `PdfLoadRequest` instead
- `PDFView.enqueue(PdfRequest)` method - Use `configure()` + `load()` pattern instead
- Migration path provided with clear deprecation warnings and `ReplaceWith` annotations

### Migration Guide

#### From Old API to New API:

```kotlin
// OLD (deprecated but still works)
val request = PdfRequest.Builder(source)
    .swipeHorizontal(true)
    .password("password")
    .defaultPage(1)
    .documentLoadListener(...)
.build()
pdfView.enqueue(request)

// NEW (recommended DSL approach)
pdfView.configureView {
    swipeHorizontal(true)
    documentLoadListener(...)
}
pdfView.loadDocument(source) {
    password("password")
    defaultPage(1)
}
```

```

#### Automatic Migration Helper:

```kotlin
// Quick migration using extension methods
val oldRequest = PdfRequest.Builder(source)...build()
pdfView.configure(oldRequest.toViewConfiguration())
pdfView.load(oldRequest.toLoadRequest())
```

#### Password Retry Example:

```kotlin
// NEW - Easy password retry with DSL
pdfView.loadDocument(file) {
    password("new_password")
}

// OLD - Complex recreation required
val newRequest = PdfRequest.Builder(source).password("new_password")...build()
pdfView.enqueue(newRequest)
```

### Benefits of New Architecture

- **🔄 Runtime Updates**: Change password, document, or pages without view reconfiguration
- **🏭 Factory Pattern**: Matches Android/Compose best practices with clear separation
- **💾 Memory Efficient**: View configuration set once, only document data changes
- **🧩 Composable**: Natural integration with Jetpack Compose AndroidView pattern
- **🚀 Performance**: Faster document switching and reduced object allocation

### Backward Compatibility

- **✅ Zero Breaking Changes**: All existing code continues to work unchanged
- **⚠️ Deprecation Warnings**: IDE will show migration suggestions with `ReplaceWith` quick fixes
- **📚 Documentation**: Complete migration examples and use cases provided
- **🛣️ Migration Path**: Gradual migration supported - use both APIs together during transition

### Technical Details

- Internal architecture completely refactored while maintaining API compatibility
- Removed internal `pdfRequest` and `pendingPdfRequest` fields from `PDFView`
- Enhanced memory management with proper separation of configuration lifecycle
- Improved error handling paths for password and document loading scenarios

## [1.0.9] - 2025-08-21

- NDK ABI filters for arm64-v8a, armeabi-v7a, x86, and x86_64 architectures to optimize APK size and
  ensure compatibility with a wider range of devices.
- Improved packaging options to disable legacy JNI library packaging and exclude unnecessary
  META-INF files, further reducing APK size.

### Changed

- **Performance Improvements**: Made PDF metadata and bookmark retrieval asynchronous using suspend
  functions to improve UI responsiveness.
    - `PdfiumCore.getDocumentMeta()` and `PdfiumCore.getTableOfContents()` are now suspend functions
    - `PdfFile.getMetaData()` and `PdfFile.getBookmarks()` are now suspend functions
    - Corresponding native methods are now executed off the main thread
- **Background Processing**: Modified sample app to load PDF files in background threads to prevent
  UI blocking.
    - `XmlActivity` now loads PDF files in a background coroutine
    - Renamed sample `setupPdfViewer` to `loadPdfFileInBackground` for better clarity
- **Code Simplification**: Simplified PDF loading logic by removing retry mechanism and limiting
  thumbnail processing for better performance.
- **PDFium Update**: Updated precompiled PDFium libraries to version 141.0.7363.0 with enhanced PDF
  rendering capabilities.

### Migration Guide

No breaking changes. This is a backward-compatible release with performance improvements.

```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.0.9'
}
```

## [1.0.8] - 2025-08-01

### Added

- Robust PDF page loading with multi-layer validation and retry mechanism to handle NaN values and
  prevent ANRs.
- Adaptive delay in PDFView for improved rendering based on document size.

### Changed

- Removed unwanted comments from PagesLoader for improved code clarity.

### Migration Guide

No migration steps required. This is a backward-compatible release.

```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.0.8'
}
```

## [1.0.7] - 2025-07-28

### Changed

- **Internal API Refactoring**: Refactored `PDFView.kt` by changing visibility of numerous
  properties and methods from public to internal for better encapsulation
    - Properties now internal: `isRecycling`, `isLoading`, `isRecycled`, `isSwipeVertical`,
      `isDoubleTapEnabled`, `isBestQuality`, `isAnnotationRendering`, `isPageFlingEnabled`,
      `scrollHandle`, `renderingHandler`
    - Methods now internal: `onPageError`, `loadPages`, `redraw`, `onBitmapRendered`, `moveTo`,
      `isActivelyScrolling`, `setScrollHandleDragging`, `updateScrollUIElements`,
      `loadPageByOffset`, `performPageSnap`, `findFocusPage`, `pageFillsScreen`, `zoomTo`,
      `resetZoomWithAnimation`, `toCurrentScale`, `isZooming`, `callOnTap`, `callOnLongPress`,
      `callLinkHandler`
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

No migration steps required. This is purely an internal refactoring release that maintains full
backward compatibility for public API users.

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
    - `DocumentValidationResult` sealed class with validation outcomes (Valid, PasswordProtected,
      Corrupted, Invalid, Error)
    - Support for various document sources (File, Uri, ByteArray, InputStream, assets)
    - Background thread validation using coroutines

- **PDF Thumbnail Generation Feature**
    - `PDFThumbnailGenerator` with `generateThumbnail()` and `generateThumbnails()` suspend
      functions
    - `ThumbnailConfig` for custom configurations (width, height, quality, aspect ratio, background
      color)
    - `ThumbnailCache` with LRU memory-aware caching system
    - `AspectRatio` enum with options: PRESERVE, STRETCH, FIT_WIDTH, FIT_HEIGHT, CROP_TO_FIT
    - `getPageCount()` utility method

- **Enhanced Sample App UI**
    - `PDFPreviewState` sealed class for state management (NoPdfSelected, PdfSelected,
      PdfPreviewing)
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
- **Issues**: Report bugs or request features
  on [GitHub Issues](https://github.com/rhariskumar3/AndroidPDFPreview/issues)
- **Discussions**: Join conversations
  on [GitHub Discussions](https://github.com/rhariskumar3/AndroidPDFPreview/discussions)

## Release Types

- **Added** for new features
- **Changed** for changes in existing functionality
- **Deprecated** for soon-to-be removed features
- **Removed** for now removed features
- **Fixed** for any bug fixes
- **Security** for security-related improvements

## Version Links

- [1.1.0](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.1.0) - Latest
- [1.0.9](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.0.9)
- [1.0.8](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.0.8)
- [1.0.7](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.0.7)
- [1.0.6](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.0.6)
- [1.0.5](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.0.5)
