# API Documentation for PDF Preview Library

This document provides detailed descriptions of the parameters, methods, and classes in the PDF
Preview library components: `PdfRequest`, `PdfViewerConfiguration`, `FitPolicy`, `SnapEdge`, and
`PdfiumException`. It serves as a reference for developers integrating the library into their
applications.

## PdfRequest

`PdfRequest` is a data class that encapsulates configuration options for loading and rendering a PDF
document. It includes settings for navigation, rendering, and event handling. This class is
typically used with the `PDFView.load()` method to customize the viewer's behavior.

### Properties

- **source** (`DocumentSource`): The source of the PDF document (e.g., file, asset, URI). Required
  for loading.
- **pageNumbers** (`List<Int>?`): Optional list of specific page numbers to load (0-based). If null,
  all pages are loaded. Useful for partial document loading.
- **enableSwipe** (`Boolean`): Enables or disables swipe gestures for page navigation. Default:
  `true`. Set to `false` to restrict manual navigation.
- **enableDoubleTap** (`Boolean`): Enables or disables double-tap gestures for zooming. Default:
  `true`. Affects zoom functionality.
- **defaultPage** (`Int`): The initial page number to display (0-based). Default: `0`. Must be
  within the document's page range.
- **swipeHorizontal** (`Boolean`): If true, swipe gestures navigate horizontally; otherwise,
  vertically. Default: `false`. Ignored if `enableSwipe` is `false`.
- **annotationRendering** (`Boolean`): Enables rendering of PDF annotations (e.g., comments,
  highlights). Default: `false`. May impact performance.
- **password** (`String?`): Password for encrypted PDFs. Default: `null`. Required if the document
  is password-protected.
- **scrollHandle** (`ScrollHandle?`): Type of scroll handle to display for navigation. Default:
  `null` (no handle). Provides visual feedback for scrolling.
- **antialiasing** (`Boolean`): Enables anti-aliasing for smoother rendering. Default: `true`.
  Improves visual quality but may affect performance.
- **spacing** (`Float`): Spacing between pages in pixels. Default: `0F`. Affects layout when
  multiple pages are visible.
- **autoSpacing** (`Boolean`): Automatically adjusts spacing based on screen size. Default: `false`.
  Overrides manual `spacing` when enabled.
- **pageFitPolicy** (`FitPolicy`): Policy for fitting page content to the screen. Default:
  `FitPolicy.WIDTH`. See `FitPolicy` section for details.
- **fitEachPage** (`Boolean`): Fits each page individually to the screen. Default: `false`. When
  `true`, each page is scaled independently.
- **pageFling** (`Boolean`): Enables page flinging for faster navigation (kinetic scrolling).
  Default: `false`. Enhances user experience on touch devices.
- **pageSnap** (`Boolean`): Enables page snapping to screen edges. Default: `false`. Helps maintain
  page alignment during scrolling.
- **scrollOptimization** (`Boolean`): Optimizes scrolling by skipping bitmap generation during fast
  scrolls. Default: `true`. May show blank areas temporarily.
- **nightMode** (`Boolean`): Inverts colors for low-light readability. Default: `false`. Useful for
  dark themes.
- **disableLongPress** (`Boolean`): Disables long-press gestures. Default: `false`. Prevents context
  menus or other long-press actions.
- **pdfViewerConfiguration** (`PdfViewerConfiguration`): Custom rendering options. Default:
  `PdfViewerConfiguration.DEFAULT`. See `PdfViewerConfiguration` section.
- **documentLoadListener** (`DocumentLoadListener?`): Listener for document load events. Default:
  `null`. Useful for handling load progress and errors.
- **renderingEventListener** (`RenderingEventListener?`): Listener for rendering events. Default:
  `null`. Notifies when pages are rendered.
- **pageNavigationEventListener** (`PageNavigationEventListener?`): Listener for page navigation
  events. Default: `null`. Tracks page changes.
- **gestureEventListener** (`GestureEventListener?`): Listener for gesture events. Default: `null`.
  Handles custom touch interactions.
- **linkHandler** (`LinkHandler?`): Handler for link clicks in the PDF. Default: `null`. Enables
  interactive links.
- **logWriter** (`LogWriter?`): Writer for logging messages and errors. Default: `null`. Aids in
  debugging.

### Builder Methods

The `Builder` class provides a fluent API to construct a `PdfRequest` instance. All methods return
the `Builder` for chaining, except `build()`.

- **pages(vararg pageNumbers: Int)**: Sets specific page numbers to load. Example: `pages(0, 2, 4)`.
- **enableSwipe(enableSwipe: Boolean)**: Sets swipe gesture enablement.
- **enableDoubleTap(doubleTap: Boolean)**: Sets double-tap enablement.
- **enableAnnotationRendering(annotationRendering: Boolean)**: Sets annotation rendering enablement.
- **defaultPage(defaultPage: Int)**: Sets the default page.
- **swipeHorizontal(swipeHorizontal: Boolean)**: Sets horizontal swipe direction.
- **password(password: String?)**: Sets the PDF password.
- **scrollHandle(scrollHandle: ScrollHandle?)**: Sets the scroll handle.
- **enableAntialiasing(antialiasing: Boolean)**: Sets anti-aliasing enablement.
- **spacing(spacing: Float)**: Sets page spacing in pixels.
- **autoSpacing(autoSpacing: Boolean)**: Sets auto-spacing enablement.
- **pageFitPolicy(pageFitPolicy: FitPolicy)**: Sets the page fit policy.
- **fitEachPage(fitEachPage: Boolean)**: Sets individual page fitting.
- **pageSnap(pageSnap: Boolean)**: Sets page snapping enablement.
- **pageFling(pageFling: Boolean)**: Sets page flinging enablement.
- **scrollOptimization(scrollOptimization: Boolean)**: Sets scroll optimization.
- **nightMode(nightMode: Boolean)**: Sets night mode enablement.
- **disableLongPress()**: Disables long-press gestures.
- **renderOptions(pdfViewerConfiguration: PdfViewerConfiguration)**: Sets custom rendering options.
- **documentLoadListener(documentLoadListener: DocumentLoadListener)**: Sets document load listener.
- **renderingEventListener(renderingEventListener: RenderingEventListener)**: Sets rendering event
  listener.
- **pageNavigationEventListener(pageNavigationEventListener: PageNavigationEventListener)**: Sets
  page navigation listener.
- **gestureEventListener(gestureEventListener: GestureEventListener)**: Sets gesture event listener.
- **linkHandler(linkHandler: LinkHandler)**: Sets link handler.
- **logWriter(logWriter: LogWriter)**: Sets log writer.
- **build()**: Constructs the `PdfRequest` instance.

**Example Usage:**

```kotlin
val request = PdfRequest.Builder(documentSource)
    .defaultPage(1)
    .enableSwipe(false)
    .pageFitPolicy(FitPolicy.BOTH)
    .build()
```

## PdfViewerConfiguration

`PdfViewerConfiguration` holds rendering options for the PDF viewer, controlling performance,
quality, and caching settings. Used within `PdfRequest` for advanced customization.

### Properties

- **isDebugEnabled** (`Boolean`): Enables debug mode for logging. Default: `false`. Useful for
  development.
- **thumbnailQuality** (`Float`): Quality of thumbnails (0-1, higher is better). Default: `0.7f`.
  Affects thumbnail clarity and memory usage.
- **renderTileSize** (`Float`): Size of rendered parts in pixels. Default: `512f`. Larger values
  improve quality but increase memory usage.
- **preloadMarginDp** (`Float`): Preload margin in dp (distance to preload content). Default: `20F`.
  Balances performance and responsiveness.
- **maxCachedBitmaps** (`Int`): Number of bitmaps to cache in memory. Default: `32`. Higher values
  improve performance but use more memory.
- **maxCachedPages** (`Int`): Maximum pages kept in view. Default: `3`. Limits memory usage for
  large documents.
- **maxCachedThumbnails** (`Int`): Number of thumbnail bitmaps to cache. Default: `4`. Optimizes
  thumbnail loading.
- **minZoom** (`Float`): Minimum zoom level (e.g., 1.0 for no zoom out). Default: `1f`. Prevents
  over-zooming out.
- **maxZoom** (`Float`): Maximum zoom level (e.g., 5.0 for 5x zoom). Default: `5f`. Limits zoom in
  to prevent performance issues.

## FitPolicy

`FitPolicy` is an enum defining how PDF pages are fitted to the view. Used in
`PdfRequest.pageFitPolicy`.

### Values

- **WIDTH**: Fits the page width to the view's width, allowing vertical scrolling if needed.
- **HEIGHT**: Fits the page height to the view's height, allowing horizontal scrolling if needed.
- **BOTH**: Fits both width and height while preserving aspect ratio, centering the page if
  necessary.

## SnapEdge

`SnapEdge` is an internal enum for snapping positions, used internally for page snapping behavior in
`PdfRequest.pageSnap`.

### Values

- **START**: Snaps to the start (left for horizontal, top for vertical).
- **CENTER**: Snaps to the center of the view.
- **END**: Snaps to the end (right for horizontal, bottom for vertical).
- **NONE**: No snapping applied.

## PdfiumException

`PdfiumException` is a sealed class for exceptions in the Pdfium library, providing specific error
types for PDF handling.

### Subclasses

- **FileNotFoundException**: Thrown when the PDF file is not found at the specified path.
- **InvalidFormatException**: Thrown when the file is not a valid PDF format.
- **IncorrectPasswordException**: Thrown when the provided password is incorrect for an encrypted
  PDF.
- **UnsupportedSecurityException**: Thrown when the PDF uses an unsupported security method.
- **PageNotFoundException**: Thrown when a requested page number does not exist in the document.
- **UnknownException(error: String)**: Thrown for unknown errors, with a custom error message.
- **PageRenderingException(page: Int, throwable: Throwable)**: Thrown during page rendering errors,
  including the page number and underlying cause.

## Additional Notes

- **Dependencies**: Ensure the Pdfium library is properly integrated. Refer to the project's README
  for setup instructions.
- **Performance Considerations**: Settings like `maxCachedBitmaps` and `renderTileSize` should be
  tuned based on device capabilities.
- **Event Handling**: Implement listeners (e.g., `DocumentLoadListener`) to handle asynchronous
  operations and user feedback.
- **Security**: Handle passwords securely and avoid logging sensitive information.
- **Testing**: Test configurations on various devices and orientations to ensure consistent
  behavior.
- **Updates**: This documentation is based on the current library version. Check for updates in the
  changelog.