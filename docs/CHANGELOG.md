# Changelog

All notable changes to AndroidPDFPreview will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.1] - 2025-10-03 - Fix password retry issue & update PDFium to chromium/7442

### Fixed

- Prevent premature resource recycling on incorrect password, allowing retries for protected PDFs.

### Changed

- Upgraded PDFium binaries to chromium/7442 for all supported architectures.
- Refactored download_pdfium_libs.sh for portability, safety, structured logging, strict error
  handling, and robust cleanup.
- Corrected README.md sample: moved documentLoadListener into loadDocument block.

## [1.2.0] - 2025-09-24 - Single Page Mode and PDFium Update

### Added

- **📄 Single Page Mode**
    - New `singlePageMode` configuration option for displaying one page at a time
    - Improved page navigation with dedicated single-page rendering logic
    - Enhanced user experience for document review workflows

- **🔄 PDFium Library Update**
    - Updated PDFium to chromium/7428 for improved PDF rendering and compatibility
    - Added automated download script for PDFium libraries
    - Enhanced stability and performance with latest PDFium features

### Changed

- **🏗️ Architecture Improvements**
    - Removed deprecated `PdfRequest` class and related methods for cleaner API
    - Refactored page positioning logic for better orientation change handling
    - Improved scroll UI updates during device rotation
    - Enhanced rendering pipeline for single page mode

- **📚 Documentation Updates**
    - Added maintenance status notice to README
    - Updated project documentation for new features
    - Improved clarity on single page mode usage

### Technical Details

- **Single Page Implementation**:
    - Dedicated rendering path for single page display
    - Optimized memory usage for single-page scenarios
    - Better zoom and pan controls for focused reading

- **PDFium Integration**:
    - Seamless upgrade to chromium/7428
    - Maintained backward compatibility with existing PDFs
    - Improved text rendering and annotation support

### Migration Guide

No breaking changes for existing users. Single page mode is an optional feature that can be enabled
via configuration.

```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.2.0'
}
```

## [1.1.1] - 2025-09-04 - Performance and Stability Improvements

### Fixed

- **🚀 Performance Optimizations**
    - Prevented redundant PDF loads in sample `PDFViewer` during recompositions
    - Added `hasLoadedPdf` state variable to track loading status and avoid unnecessary reloads
    - Early return logic in AndroidView's update lambda when PDF is already loaded
    - Reset loading flag to `false` on document load errors to allow retry scenarios

- **🔧 Loading State Management**
    - Improved `isCurrentlyLoading` flag usage to prevent concurrent load operations
    - Enhanced `pendingLoadRequest` handling with immediate setting and proper cleanup
    - More reliable reset of loading flags (`isLoading`, `isCurrentlyLoading`, `currentLoadingJob`,
      `pendingLoadRequest`)
    - Consistent state management in `loadComplete`, `loadError`, and during cancellation

- **🛡️ Robust Cancellation and Lifecycle**
    - Introduced `Job.cancelSafely()` extension to handle exceptions during job cancellation
    - Improved `recycle()` and `onDetachedFromWindow()` with safe job cancellation
    - Better early exit checks in `loadDoc()` for recycled views or cancelled jobs
    - Removed unnecessary `ensureActive()` calls in favor of explicit cancellation handling

- **📐 Enhanced Size Change Logic**
    - Prevented new load attempts when loading is already in progress during `onSizeChanged`
    - Refined logging for size change scenarios with valid dimensions but no pending requests
    - Better handling of view dimension changes to prevent race conditions

- **⚠️ Error Handling Improvements**
    - Ensured `onDocumentLoadError()` is called before `recycle()` in error scenarios
    - Added null safety checks in `loadComplete()` before attempting `jumpTo`
    - Standardized logging with consistent "PDFView" tag usage
    - Enhanced debug logging in sample app for AndroidView factory and update calls

### Technical Details

- **Internal State Improvements**:
    - Better separation of concerns between loading states and view lifecycle
    - Improved error recovery paths with proper state cleanup
    - Enhanced logging for debugging PDF loading and navigation issues

- **Sample App Enhancements**:
    - Added state tracking in `PDFViewer` composable to prevent redundant loads
    - Improved recomposition handling with file-based state keys
    - Better error handling and retry scenarios for password-protected PDFs

### Migration Guide

No migration steps required. This is a backward-compatible release focused on performance and
stability improvements.

```gradle
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.1.1'
}
```

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

- **🛠️ DSL Extension Functions**
    - `configureView { }` extension for PDFView configuration
    - `loadDocument { }` extension for document loading
    - Fluent builder API for improved developer experience

- **📋 Builder Pattern Support**
    - `PdfViewConfiguration.Builder` for programmatic configuration
    - `PdfLoadRequest.Builder` for load request construction
    - Consistent API patterns across all configuration classes

- **📄 New Documentation Files**
    - `API_Documentation.md` - Comprehensive API reference with examples
    - `Use_Case_Implementations.md` - Practical implementation scenarios and patterns

### Changed

- **📋 API Evolution**
    - `PdfRequest` and `PDFView.enqueue()` method marked as deprecated (but fully supported)
    - Improved error handling with clearer exception paths
    - Better separation between factory-time and runtime concerns

- **🔧 Internal Architecture Improvements**
    - Deferred PDF loading until view dimensions are available (prevents loading with invalid size)
    - Enhanced logging in `jumpTo` method for better debugging of page navigation issues
    - Added null safety checks in `showPage` method to prevent NullPointerExceptions
    - Improved view size validation before PDF processing begins

- **📚 Documentation Structure**
    - Updated spacing documentation to clarify units (dp instead of pixels)
    - Enhanced copyright headers updated to 2025 across all source files
    - Comprehensive documentation reorganization with dedicated API and use case files

- **🏭 Build and Configuration Updates**
    - Version management system updated to support new architecture
    - Sample app (`PDFViewer.kt`) updated with new configuration patterns and improved examples

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

### Fixed

- **🛡️ Robustness Improvements**
    - Fixed potential issues with PDF loading before view initialization
    - Added null safety checks in `showPage` method to prevent crashes when called before PDF is
      loaded
    - Improved handling of view size changes to prevent unnecessary PDF reloading
    - Enhanced error handling in deferred loading scenarios

- **📱 View Lifecycle Management**
    - Proper cleanup of `pendingPdfRequest` in `recycle()` and `onDetachedFromWindow()`
    - Better state management during view dimension changes
    - Prevented PDF processing attempts with invalid view dimensions (width/height = 0)

### Technical Details

- **Internal Architecture Changes**:
    - Removed internal `pdfRequest` and `pendingPdfRequest` fields from public API
    - Enhanced memory management with proper separation of configuration lifecycle
    - Improved error handling paths for password and document loading scenarios
    - Added comprehensive logging for debugging PDF loading and navigation issues

- **New Internal State Management**:
    - `pendingPdfRequest: PdfRequest?` for deferred loading until view is ready
    - Enhanced `onSizeChanged` logic to process deferred requests only when appropriate
    - Refined conditions for PDF reloading on size changes to prevent blank screens

- **API Compatibility Layer**:
    - Migration extension methods (`toViewConfiguration()` and `toLoadRequest()`) for smooth
      transition
    - Complete backward compatibility maintained through deprecation annotations with `ReplaceWith`
    - Both old and new APIs can be used simultaneously during migration period

### Files Modified

This major refactor touched 40 files with 2,280 additions and 266 deletions:

- **New Classes Added**:
    - `PdfLoadRequest.kt` - Runtime document loading configuration
    - `PdfViewConfiguration.kt` - Factory-time view configuration

- **Core Classes Modified**:
    - `PDFView.kt` - Major refactoring with new configure()/load() methods and improved state
      management
    - `Extensions.kt` - Added DSL extension functions (`configureView`, `loadDocument`)
    - `PdfRequest.kt` - Deprecated with migration helper methods

- **Documentation Files**:
    - `API_Documentation.md` - New comprehensive API reference (368+ lines)
    - `Use_Case_Implementations.md` - New practical implementation guide (602+ lines)
    - `README.md` - Major update with new architecture examples (261+ lines added)
    - `CHANGELOG.md`, `VERSION_HISTORY.md`, `docs/README.md` - Updated for v1.1.0

- **Build and Configuration**:
    - `gradle.properties`, `app/build.gradle.kts` - Version updates
    - Sample app `PDFViewer.kt` - Updated with new patterns (215 lines modified)

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

- [1.2.0](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.2.0) - Latest
- [1.1.1](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.1.1)
- [1.1.0](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.1.0)
- [1.0.9](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.0.9)
- [1.0.8](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.0.8)
- [1.0.7](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.0.7)
- [1.0.6](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.0.6)
- [1.0.5](https://github.com/rhariskumar3/AndroidPDFPreview/releases/tag/v1.0.5)
