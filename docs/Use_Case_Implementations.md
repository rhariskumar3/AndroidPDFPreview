# Use Case Implementations for PDF Preview Library

This document provides practical implementations for common use cases when using the PDF Preview
library. Each section includes configuration examples using both the new recommended architecture (
`PdfViewConfiguration` + `PdfLoadRequest`) and the legacy approach (`PdfRequest.Builder`) for
backward compatibility. Refer to `API_Documentation.md` for detailed parameter descriptions.

## New Architecture Overview

The library now supports two configuration approaches:

### **Recommended: Separate Configuration**

- **Factory-time**: `PdfViewConfiguration` - view behavior, set once
- **Runtime**: `PdfLoadRequest` - document loading, can change
- **Methods**: `configure()` + `load()`

### **Legacy: Unified Configuration**

- **All-in-one**: `PdfRequest` - all settings combined (deprecated)
- **Method**: `enqueue()` (deprecated but supported)

## 1. Single Page Mode

Enable single page mode to display one page at a time with automatic scroll constraints and optimized performance. This provides an e-book reader-like experience where users see exactly one complete page without adjacent page visibility.

### New API (Recommended)

```kotlin
// Configure view behavior once
val viewConfig = PdfViewConfiguration.Builder()
    .singlePageMode(true)  // Enable single page mode
    .pageFitPolicy(FitPolicy.BOTH)  // Fit entire page to view
    .enableSwipe(true)  // Allow swipe gestures for page navigation
    .swipeHorizontal(false)  // Vertical scrolling between pages
    .scrollHandle(DefaultScrollHandle(context))  // Optional scroll handle
    .documentLoadListener(object : DocumentLoadListener {
        override fun onDocumentLoaded(totalPages: Int) {
            // Document loaded successfully
            println("Loaded $totalPages pages in single page mode")
        }
        override fun onDocumentLoadError(error: Throwable) {
            // Handle load errors
        }
    })
    .pageNavigationEventListener(object : PageNavigationEventListener {
        override fun onPageChanged(newPage: Int, pageCount: Int) {
            // Page changed - update UI indicators
            updatePageIndicator(newPage + 1, pageCount)
        }
    })
    .build()

pdfView.configure(viewConfig)

// Load document
pdfView.loadDocument(documentSource) {
    defaultPage(0)  // Start at first page
}
```

### Legacy API (Deprecated)

```kotlin
val request = PdfRequest.Builder(documentSource)
    .singlePageMode(true)  // Enable single page mode
    .pageFitPolicy(FitPolicy.BOTH)  // Fit entire page to view
    .enableSwipe(true)  // Allow swipe gestures
    .swipeHorizontal(false)  // Vertical scrolling
    .defaultPage(0)  // Start at first page
    .documentLoadListener(object : DocumentLoadListener {
        override fun onDocumentLoaded(totalPages: Int) {
            // Document loaded
        }
        override fun onDocumentLoadError(error: Throwable) {
            // Handle errors
        }
    })
    .pageNavigationEventListener(object : PageNavigationEventListener {
        override fun onPageChanged(newPage: Int, pageCount: Int) {
            // Page changed
        }
    })
    .build()

@Suppress("DEPRECATION")
pdfView.enqueue(request)
```

### Explanation

- `singlePageMode(true)` enables the single page viewing experience
- Scrolling is automatically constrained to page boundaries
- Only the current page is rendered, improving performance
- Scroll handles show correct page numbers and disable inappropriate navigation
- Zooming works within the current page boundaries

### Key Features of Single Page Mode

- **📖 E-book Experience**: Clean, distraction-free reading with one page at a time
- **⚡ Performance Optimized**: Only renders current page, reducing memory usage
- **🎯 Precise Navigation**: Automatic snapping to page boundaries during scrolling
- **🔄 Zoom Support**: Zoom within page while maintaining single-page constraints
- **📱 Responsive**: Works in both portrait and landscape orientations
- **🎨 UI Adaptation**: Scroll handles and navigation controls adapt automatically

### Best Practices

- Use `FitPolicy.BOTH` for best single-page experience
- Implement page navigation listeners to update UI indicators
- Consider adding custom navigation controls for enhanced UX
- Test on various screen sizes and orientations
- Monitor performance improvements with large documents

## 2. Two Pages Per View Handling

Configure for viewing two pages side by side (e.g., for book-like reading), with horizontal swiping
enabled.

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

Handle password-protected PDFs, including prompting for passwords and managing exceptions. The new
architecture makes password retry scenarios much easier.

### New API (Recommended) - Password Retry Made Easy

```kotlin
class PDFPasswordActivity : AppCompatActivity() {
    private lateinit var pdfView: PDFView
    private lateinit var currentLoadRequest: PdfLoadRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure view behavior once
        val viewConfig = PdfViewConfiguration.Builder()
            .swipeHorizontal(true)
            .enableAnnotationRendering(true)
            .documentLoadListener(object : DocumentLoadListener {
                override fun onDocumentLoaded(totalPages: Int) {
                    // Success! Hide loading, show content
                    hidePasswordDialog()
                    showSuccessMessage("Document loaded: $totalPages pages")
                }

                override fun onDocumentLoadError(error: Throwable) {
                    when (error) {
                        is IncorrectPasswordException -> {
                            // Show password retry dialog
                            showPasswordDialog(isRetry = true)
                        }
                        is UnsupportedSecurityException -> {
                            showError("This document uses unsupported encryption")
                        }
                        else -> {
                            showError("Failed to load document: ${error.message}")
                        }
                    }
                }
            })
            .build()

        pdfView.configure(viewConfig)

        // Initial load attempt (no password)
        pdfView.loadDocument(documentSource)
    }

    private fun showPasswordDialog(isRetry: Boolean = false) {
        val title = if (isRetry) "Incorrect Password. Try Again:" else "Enter Password:"

        // Show password input dialog
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(passwordInputView)
            .setPositiveButton("OK") { _, _ ->
                val password = passwordEditText.text.toString()
                retryWithPassword(password)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish() // Or handle cancellation
            }
            .show()
    }

    private fun retryWithPassword(password: String) {
        // Easy password retry - just update the load request!
        pdfView.loadDocument(documentSource) {
            password(password)
        }
    }

    // Example: Allow user to switch documents while keeping password
    private fun loadDifferentDocument(newSource: DocumentSource, currentPassword: String?) {
        pdfView.loadDocument(newSource) {
            password(currentPassword)  // Reuses the same password
        }
    }
}
```

### Legacy API (Deprecated) - More Complex

```kotlin
class PDFPasswordActivityLegacy : AppCompatActivity() {
    private var currentPassword: String? = null

    private fun loadWithPassword(password: String?) {
        currentPassword = password

        val request = PdfRequest.Builder(documentSource)
            .password(password)
            .swipeHorizontal(true)
            .enableAnnotationRendering(true)
            .documentLoadListener(object : DocumentLoadListener {
                override fun onDocumentLoaded(totalPages: Int) {
                    // Success
                }
                override fun onDocumentLoadError(error: Throwable) {
                    when (error) {
                        is IncorrectPasswordException -> {
                            showPasswordDialog(isRetry = true)
                        }
                        else -> {
                            showError("Load error: ${error.message}")
                        }
                    }
                }
            })
            .build()

        @Suppress("DEPRECATION")
        pdfView.enqueue(request)
    }

    private fun retryWithPassword(password: String) {
        // Must recreate entire request - more complex!
        loadWithPassword(password)
    }
}
```

### Explanation

- Set `password` if available; otherwise, handle `IncorrectPasswordException` to prompt user.
- **New API Advantage**: Easy password retry with `copy()` method
- **Legacy Limitation**: Must recreate entire `PdfRequest` for password retry

### Best Practices

- Securely store and handle passwords.
- Provide user-friendly error messages.
- Implement retry logic with limits to prevent brute-force attempts.
- Use the new architecture for password-protected document scenarios.

## 8. Document Switching and Password Management (New Architecture Only)

This use case demonstrates the power of the new architecture for managing multiple documents with
different passwords.

### Implementation

```kotlin
@Composable
fun DocumentSwitcher(
    documents: List<DocumentInfo> // DocumentInfo(source, name, password?)
) {
    var selectedDoc by remember { mutableStateOf(documents.firstOrNull()) }
    var currentPassword by remember { mutableStateOf<String?>(null) }
    var isPasswordDialogVisible by remember { mutableStateOf(false) }

    selectedDoc?.let { doc ->
        AndroidView(
            factory = { context ->
                PDFView(context).apply {
                    // Configure view behavior once
                    val viewConfig = PdfViewConfiguration.Builder()
                        .swipeHorizontal(true)
                        .enableAnnotationRendering(true)
                        .documentLoadListener(object : DocumentLoadListener {
                            override fun onDocumentLoaded(totalPages: Int) {
                                // Success - remember password for this document
                                if (currentPassword != null) {
                                    doc.rememberedPassword = currentPassword
                                }
                            }

                            override fun onDocumentLoadError(error: Throwable) {
                                if (error is IncorrectPasswordException) {
                                    isPasswordDialogVisible = true
                                }
                            }
                        })
                        .build()

                    configure(viewConfig)
                }
            },
            update = { pdfView ->
                // Load document when selection changes
                pdfView.loadDocument(doc.source) {
                    password(doc.rememberedPassword ?: currentPassword)
                }
            }
        )

        // Document selector
        LazyRow {
            items(documents) { document ->
                Button(
                    onClick = {
                        selectedDoc = document
                        currentPassword = document.rememberedPassword
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (document == selectedDoc)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(document.name)
                }
            }
        }

        // Password dialog
        if (isPasswordDialogVisible) {
            PasswordDialog(
                onPasswordEntered = { password ->
                    currentPassword = password
                    isPasswordDialogVisible = false
                    // Recomposition will trigger document reload with new password
                },
                onDismiss = { isPasswordDialogVisible = false }
            )
        }
    }
}

data class DocumentInfo(
    val source: DocumentSource,
    val name: String,
    var rememberedPassword: String? = null
)

@Composable
fun PasswordDialog(
    onPasswordEntered: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Password") },
        text = {
            TextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password") },
                visualTransformation = PasswordVisualTransformation()
            )
        },
        confirmButton = {
            Button(onClick = { onPasswordEntered(password) }) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### Advantages of New Architecture

- **Easy Document Switching**: Just change the `PdfLoadRequest`
- **Password Persistence**: Remember passwords per document
- **Compose Integration**: Works naturally with recomposition
- **Performance**: View configuration stays constant, only document data changes

## 9. Runtime Page Selection (New Architecture Feature)

Load different page ranges from the same document without reconfiguring the view.

### Implementation

```kotlin
class PageSelectorActivity : AppCompatActivity() {
    private lateinit var pdfView: PDFView
    private lateinit var documentSource: DocumentSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure view once
        val viewConfig = PdfViewConfiguration.Builder()
            .swipeHorizontal(true)
            .pageFitPolicy(FitPolicy.WIDTH)
            .build()

        pdfView.configure(viewConfig)

        // Load all pages initially
        loadPages(null) // null = all pages
    }

    private fun loadPages(pageNumbers: List<Int>?) {
        if (pageNumbers != null) {
            pdfView.loadDocument(documentSource) {
                pages(*pageNumbers.toIntArray())
            }
        } else {
            // Load all pages
            pdfView.loadDocument(documentSource)
        }
    }

    // UI actions
    private fun showAllPages() = loadPages(null)
    private fun showOddPages() = loadPages(listOf(0, 2, 4, 6, 8)) // Pages 1, 3, 5, 7, 9
    private fun showEvenPages() = loadPages(listOf(1, 3, 5, 7, 9)) // Pages 2, 4, 6, 8, 10
    private fun showFirstFive() = loadPages(listOf(0, 1, 2, 3, 4))
}
```

## 5. Zoom and Annotation Handling

Enable zoom controls and annotation rendering for interactive viewing.

### Configuration

```kotlin
val request = PdfRequest.Builder(documentSource)
    .enableDoubleTap(true)  // Enable double-tap zoom
    .renderOptions(
        PdfViewerConfiguration(
            minZoom = 1f,
            maxZoom = 3f,  // Limit zoom levels
            isDebugEnabled = false
        )
    )
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
    .renderOptions(
        PdfViewerConfiguration(
            renderTileSize = 256f,  // Smaller tiles for memory efficiency
            maxCachedBitmaps = 16,  // Reduce cache
            preloadMarginDp = 10f  // Smaller preload area
        )
    )
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

### Migration from Legacy API

- **Gradual Migration**: Both APIs work together - migrate incrementally
- **Extension Methods**: Use `pdfRequest.toViewConfiguration()` and `pdfRequest.toLoadRequest()` for
  automatic conversion
- **Testing**: Test new architecture alongside existing code

### New Architecture Benefits

- **Password Retry**: Update password without losing view state
- **Document Switching**: Change documents while preserving view configuration
- **Compose Integration**: Natural factory/update pattern support
- **Performance**: View settings cached, only document data changes
- **Memory Efficiency**: Reduced object creation for repeated loads

### General Best Practices

- **Error Handling**: Always implement `DocumentLoadListener` and `RenderingEventListener` for
  robust apps.
- **Testing**: Test configurations on different devices, orientations, and PDF types.
- **Security**: Avoid logging sensitive data like passwords.
- **Updates**: Check library updates for new features or bug fixes.
- **Integration**: Combine multiple use cases as needed (e.g., single page with zoom).
- **Architecture**: Prefer the new `configure()` + `load()` pattern for new projects.