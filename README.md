<p align="center">
   <a href="https://rhariskumar3.github.io/AndroidPDFPreview/">
     <img alt="AndroidPDFPreview" src=".github/logo.png" />
   </a>
</p>

![GitHub License](https://img.shields.io/github/license/rhariskumar3/AndroidPDFPreview)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.rhariskumar3/pdfpreview)](https://central.sonatype.com/artifact/io.github.rhariskumar3/pdfpreview)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.23-orange.svg)](http://kotlinlang.org/)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/rhariskumar3/AndroidPDFPreview/android.yml)
![GitHub Issues](https://img.shields.io/github/issues/rhariskumar3/AndroidPDFPreview)

AndroidPDFPreview is a lightweight and easy-to-use library for displaying and interacting with PDFs
in your Android applications. Built on top of PdfiumAndroid for decoding and AndroidPdfViewer for
rendering, it delivers a smooth and user-friendly experience.

## Features

* Display and interact with PDF documents
* Generate thumbnails from PDF pages for preview purposes
* Validate PDF documents for integrity, password protection, and corruption
* Support for gestures, zoom, and double tap
* Lightweight and easy to integrate into your Android apps
* Compatible with Android versions 5.1 and above

## Installation

To install AndroidPDFPreview, add the following dependency to your project's Gradle file:

```
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.0.9'
}
```

## Quick Start

1. Add a `PDFView` to your layout:

   XML

    ```
    <com.harissk.pdfpreview.PDFView
        android:id="@+id/pdf_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
    ```

2. Load a PDF in your code:

   Kotlin
    ```
    // Optional: Validate document first (recommended)
    val isValid = PDFThumbnailGenerator.isDocumentValid(this, file)
    if (!isValid) {
        // Handle invalid document
        return
    }
    
    binding.pdfView.load(file) {
        defaultPage(0)
        swipeHorizontal(true)
        enableAnnotationRendering(true)
        spacing(10F) // in dp
    
        // Listeners
        documentLoadListener(...)
        renderingEventListener(...)
        pageNavigationEventListener(...)
        gestureEventListener(...)
        linkHandler(...)
    }
    ```

Use code with caution. Learn more

## Jetpack Compose Usage

You can also use PDFView in Jetpack Compose with `AndroidView`:

```kotlin
@Composable
fun PDFViewCompose(
    file: File,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PDFView(context).apply {
                load(file) {
                    defaultPage(0)
                    swipeHorizontal(true)
                    enableAnnotationRendering(true)
                    spacing(10F) // in dp

                    // Listeners
                    documentLoadListener { pages ->
                        // Document loaded with $pages pages
                    }
                    renderingEventListener { page ->
                        // Page $page rendered
                    }
                    pageNavigationEventListener { page, pageCount ->
                        // Navigated to page $page of $pageCount
                    }
                    gestureEventListener { type ->
                        // Gesture event: $type
                    }
                    linkHandler { uri ->
                        // Handle link: $uri
                    }
                }
            }
        }
    )
}

// Usage in your Composable
@Composable
fun MyScreen() {
    val pdfFile = remember { /* your file */ }

    PDFViewCompose(
        file = pdfFile,
        modifier = Modifier.fillMaxSize()
    )
}
```

### Going Further:

* Asset
* File
* Uri
* ByteArray
* InputStream
* DocumentSource (custom)

## PDF Thumbnail Generation

Generate thumbnails from PDF documents for preview purposes:

```kotlin
// Simple thumbnail generation
val thumbnail = PDFThumbnailGenerator.generateThumbnail(
    context = this,
    source = pdfFile,
    pageIndex = 0
)
thumbnail?.let { imageView.setImageBitmap(it) }

// Custom thumbnail configuration
val config = ThumbnailConfig(
    width = 300,
    height = 400,
    quality = Bitmap.Config.ARGB_8888,
    annotationRendering = true,
    aspectRatio = AspectRatio.PRESERVE
)

val thumbnail = PDFThumbnailGenerator.generateThumbnail(
    context = this,
    source = pdfFile,
    pageIndex = 0,
    config = config
)

// Generate multiple thumbnails
val thumbnails = PDFThumbnailGenerator.generateThumbnails(
    context = this,
    source = pdfFile,
    pageIndices = listOf(0, 1, 2, 3)
)

// Get page count
val pageCount = PDFThumbnailGenerator.getPageCount(context, pdfFile)
```

### Jetpack Compose Integration:

```kotlin
@Composable
fun PDFThumbnailImage(
    file: File,
    page: Int = 0,
    size: Int = 200,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(file, page, size) {
        bitmap = PDFThumbnailGenerator.generateThumbnail(
            context = context,
            source = file,
            pageIndex = page,
            config = ThumbnailConfig(width = size, height = size)
        )
    }

    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "PDF Thumbnail",
            modifier = modifier
        )
    }
}
```

## PDF Document Validation

Validate PDF documents before processing to check if they're valid, password protected, or
corrupted:

```kotlin
// Comprehensive validation with detailed results
val validationResult = PDFThumbnailGenerator.validateDocument(context, pdfFile)

when (validationResult) {
    is DocumentValidationResult.Valid -> {
        println("✅ Document is valid with ${validationResult.pageCount} pages")
        println("Has metadata: ${validationResult.hasMetadata}")
        println("Has bookmarks: ${validationResult.hasBookmarks}")

        // Safe to generate thumbnails or open document
        val thumbnail = PDFThumbnailGenerator.generateThumbnail(context, pdfFile)
    }

    is DocumentValidationResult.PasswordProtected -> {
        println("🔒 Document requires password")
        println("Security level: ${validationResult.securityLevel}")

        // Request password from user
        val password = showPasswordDialog()
        val thumbnail = PDFThumbnailGenerator.generateThumbnail(
            context, pdfFile, password = password
        )
    }

    is DocumentValidationResult.Corrupted -> {
        println("❌ Document is corrupted: ${validationResult.reason}")
        println("Error code: ${validationResult.errorCode}")
        showErrorDialog("Document cannot be opened: corrupted or damaged")
    }

    is DocumentValidationResult.Invalid -> {
        println("⚠️ Document is invalid: ${validationResult.reason}")
        showErrorDialog("Invalid PDF: ${validationResult.errorMessage}")
    }

    is DocumentValidationResult.Error -> {
        println("🚫 Validation error: ${validationResult.errorMessage}")
        showErrorDialog("Cannot validate document")
    }
}

// Quick validation methods
val isValid = PDFThumbnailGenerator.isDocumentValid(context, pdfFile)
val needsPassword = PDFThumbnailGenerator.isPasswordProtected(context, pdfFile)
val correctPassword = PDFThumbnailGenerator.isPasswordCorrect(context, pdfFile, "password123")
```

### Validation Features:

- **📋 Document Status**: Valid, password protected, corrupted, or invalid
- **🔍 Detailed Analysis**: Page count, metadata presence, bookmarks detection
- **🔐 Security Detection**: Password protection and encryption level identification
- **⚡ Efficient Operations**: All validation runs on background threads
- **🎯 Multiple Sources**: Supports File, Uri, ByteArray, InputStream, and assets
- **🛡️ Error Handling**: Comprehensive error detection with specific error codes

### Use Cases:

1. **Pre-validation**: Check documents before thumbnail generation or viewing
2. **Password Handling**: Detect and handle password-protected PDFs
3. **Error Prevention**: Avoid crashes from corrupted or invalid files
4. **User Experience**: Provide specific error messages for different issues
5. **Performance**: Quick boolean checks for simple validation needs

## Additional Features

AndroidPDFPreview supports a number of additional features, including:

* Page navigation
* **PDF Thumbnail Generation** - Generate preview thumbnails from any page
* **PDF Document Validation** - Validate documents for integrity, password protection, and
  corruption
* Search (coming soon)
* Table of contents
* Bookmarks
* Annotations (coming soon)

## Contributing

We welcome contributions! Raise pull requests or file issues on GitHub.

## Contact

Have questions or feedback? Reach out to us at [https://github.com/rhariskumar3/AndroidPDFPreview]

## Acknowledgements

Thanks to these projects:

* [Pre-compiled binaries of PDFium](https://github.com/bblanchon/pdfium-binaries)
* [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer)

We hope you find AndroidPDFPreview to be a valuable tool for developing your Android apps.

## License

    Copyright 2023-2025 AndroidPDFPreview Contributors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

** Happy coding! **
