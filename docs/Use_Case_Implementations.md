# Use Case Implementations for PDF Preview Library

This document provides practical implementations for common use cases when using the PDF Preview library. Each section includes configuration examples using `PdfRequest.Builder`, explanations, and best practices. Refer to `API_Documentation.md` for detailed parameter descriptions.

## 1. Single Page Handling

Configure the viewer to display one page at a time, disable user swiping, and enable programmatic navigation (e.g., via buttons or `jumpTo`).

### Configuration
```kotlin
val request = PdfRequest.Builder(documentSource)
    .fitEachPage(true)  // Fit each page individually
    .pageFitPolicy(FitPolicy.BOTH)  // Fit entire page to view
    .enableSwipe(false)  // Disable swipe gestures
    .swipeHorizontal(false)  // Ensure no horizontal swiping
    .pageSnap(true)  // Snap to page boundaries
    .defaultPage(0)  // Start at first page
    .documentLoadListener(object : DocumentLoadListener {
        override fun onDocumentLoaded(totalPages: Int) {
            // Handle document loaded, e.g., update UI with total pages
        }
        override fun onDocumentLoadError(error: Throwable) {
            // Handle load errors
        }
    })
    .pageNavigationEventListener(object : PageNavigationEventListener {
        override fun onPageChanged(newPage: Int, pageCount: Int) {
            // Update UI or perform actions on page change
        }
    })
    .build()
```

### Explanation
- `fitEachPage(true)` ensures each page is fitted to the view.
- `enableSwipe(false)` prevents manual navigation.
- Use `pdfView.jumpTo(page)` programmatically for navigation.
- Add UI controls (e.g., next/previous buttons) that call `jumpTo`.

### Best Practices
- Implement page change listeners to update UI state.
- Handle edge cases like first/last page navigation.

## 2. Two Pages Per View Handling

Configure for viewing two pages side by side (e.g., for book-like reading), with horizontal swiping enabled.

### Configuration
```kotlin
val request = PdfRequest.Builder(documentSource)
    .fitEachPage(false)  // Allow multiple pages in view
    .pageFitPolicy(FitPolicy.WIDTH)  // Fit width for side-by-side
    .enableSwipe(true)  // Enable swiping
    .swipeHorizontal(true)  // Horizontal swiping for page pairs
    .pageSnap(true)  // Snap to page boundaries
    .spacing(10f)  // Space between pages
    .defaultPage(0)  // Start at first page
    .build()
```

### Explanation
- `fitEachPage(false)` allows multiple pages in the view.
- `swipeHorizontal(true)` enables horizontal navigation.
- Adjust `spacing` for visual separation between pages.
- For even/odd page pairing, handle programmatically in navigation logic.

### Best Practices
- Test on different screen sizes to ensure proper layout.
- Use `pageNavigationEventListener` to track current page pair.

## 3. Disable Swipe and Programmatic Scrolling

Disable all user gestures and rely on programmatic scrolling/navigation for controlled access.

### Configuration
```kotlin
val request = PdfRequest.Builder(documentSource)
    .enableSwipe(false)  // Disable swipe gestures
    .enableDoubleTap(false)  // Disable double-tap zoom
    .scrollHandle(null)  // No scroll handle
    .disableLongPress()  // Disable long-press
    .gestureEventListener(object : GestureEventListener {
        override fun onTap(motionEvent: MotionEvent): Boolean {
            // Consume all taps to prevent actions
            return true
        }
        override fun onLongPress(motionEvent: MotionEvent) {
            // Handle or ignore long press
        }
    })
    .build()
```

### Explanation
- All user interactions are disabled.
- Use `pdfView.jumpTo(page)` or `pdfView.scrollTo(page, positionOffset)` for navigation.
- Implement custom UI controls for scrolling.

### Best Practices
- Provide clear UI feedback for programmatic actions.
- Ensure accessibility with keyboard or button navigation.

## 4. Password Exception Handling

Handle password-protected PDFs, including prompting for passwords and managing exceptions.

### Configuration
```kotlin
val request = PdfRequest.Builder(documentSource)
    .password(userProvidedPassword)  // Set password if known
    .documentLoadListener(object : DocumentLoadListener {
        override fun onDocumentLoaded(totalPages: Int) {
            // Document loaded successfully
        }
        override fun onDocumentLoadError(error: Throwable) {
            when (error) {
                is IncorrectPasswordException -> {
                    // Prompt user for password
                    // Retry with new password
                }
                is UnsupportedSecurityException -> {
                    // Handle unsupported security
                }
                else -> {
                    // Handle other errors
                }
            }
        }
    })
    .build()
```

### Explanation
- Set `password` if available; otherwise, handle `IncorrectPasswordException` to prompt user.
- Use try-catch or listener to manage `PdfiumException` subclasses.
- For retry, create a new `PdfRequest` with the correct password.

### Best Practices
- Securely store and handle passwords.
- Provide user-friendly error messages.
- Implement retry logic with limits to prevent brute-force attempts.

## 5. Zoom and Annotation Handling

Enable zoom controls and annotation rendering for interactive viewing.

### Configuration
```kotlin
val request = PdfRequest.Builder(documentSource)
    .enableDoubleTap(true)  // Enable double-tap zoom
    .renderOptions(PdfViewerConfiguration(
        minZoom = 1f,
        maxZoom = 3f,  // Limit zoom levels
        isDebugEnabled = false
    ))
    .enableAnnotationRendering(true)  // Render annotations
    .nightMode(false)  // Or true for dark mode
    .build()
```

### Explanation
- Customize zoom via `PdfViewerConfiguration`.
- `enableAnnotationRendering(true)` shows PDF annotations.
- `nightMode` inverts colors for better readability.

### Best Practices
- Balance zoom limits with performance.
- Test annotation rendering on various PDF types.

## 6. Performance Optimization for Large Documents

Configure for efficient loading and rendering of large PDFs.

### Configuration
```kotlin
val request = PdfRequest.Builder(documentSource)
    .renderOptions(PdfViewerConfiguration(
        renderTileSize = 256f,  // Smaller tiles for memory efficiency
        maxCachedBitmaps = 16,  // Reduce cache
        preloadMarginDp = 10f  // Smaller preload area
    ))
    .scrollOptimization(true)  // Enable scroll optimization
    .antialiasing(false)  // Disable for performance
    .build()
```

### Explanation
- Adjust `PdfViewerConfiguration` for lower memory usage.
- `scrollOptimization` skips rendering during fast scrolls.

### Best Practices
- Monitor memory usage on target devices.
- Use `thumbnailQuality` for faster thumbnail loading.

## 7. Custom Gesture Handling

Implement custom touch interactions, such as single-tap navigation.

### Configuration
```kotlin
val request = PdfRequest.Builder(documentSource)
    .gestureEventListener(object : GestureEventListener {
        override fun onTap(motionEvent: MotionEvent): Boolean {
            // Custom tap handling, e.g., next page on right tap
            if (motionEvent.x > viewWidth / 2) {
                pdfView.jumpTo(currentPage + 1)
            } else {
                pdfView.jumpTo(currentPage - 1)
            }
            return true
        }
        override fun onLongPress(motionEvent: MotionEvent) {
            // Custom long press, e.g., show menu
        }
    })
    .build()
```

### Explanation
- Override `GestureEventListener` for custom behavior.
- Return `true` to consume events.

### Best Practices
- Ensure custom gestures don't conflict with accessibility.
- Test on various devices for consistent behavior.

## Additional Tips

- **Error Handling**: Always implement `DocumentLoadListener` and `RenderingEventListener` for robust apps.
- **Testing**: Test configurations on different devices, orientations, and PDF types.
- **Security**: Avoid logging sensitive data like passwords.
- **Updates**: Check library updates for new features or bug fixes.
- **Integration**: Combine multiple use cases as needed (e.g., single page with zoom).