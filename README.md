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

## Maintenance Status

**Note:** This library is no longer actively maintained. Google is developing an official PDF
library for Android as part of Jetpack. For new projects, consider using
the [Android PDF library](https://developer.android.com/jetpack/androidx/releases/pdf) when it
becomes available. This repository will remain available for existing users but will not receive new
features or active bug fixes.

## Features

* Display and interact with PDF documents
* Generate thumbnails from PDF pages for preview purposes
* Validate PDF documents for integrity, password protection, and corruption
* Support for gestures, zoom, and double tap
* **Single page mode** for e-book style reading with one page at a time
* Lightweight and easy to integrate into your Android apps
* Compatible with Android versions 5.1 and above
* **16KB page size support** for Android 15+ devices

## Installation

To install AndroidPDFPreview, add the following dependency to your project's Gradle file:

```
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.2.1'
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

   ```kotlin
   // Optional: Validate document first (recommended)
   val isValid = PDFThumbnailGenerator.isDocumentValid(this, file)
   if (!isValid) {
       // Handle invalid document
       return
   }
   
   // Configure view settings (factory-time, set once)
   binding.pdfView.configureView {
       swipeHorizontal(true)
       enableAnnotationRendering(true)
       singlePageMode(true)  // Enable single page mode for e-book experience
       spacing(10F) // in dp
       renderingEventListener(...)
       pageNavigationEventListener(...)
       gestureEventListener(...)
       linkHandler(...)
   }
   
   // Load document (runtime, can be called multiple times)
   binding.pdfView.loadDocument(file) {
       defaultPage(0)
       password(null) // Can be updated for password retry
       documentLoadListener(...)
   }
   ```

## Jetpack Compose Usage

You can use PDFView in Jetpack Compose with `AndroidView`.
See [Use Case Implementations](docs/Use_Case_Implementations.md) for detailed examples.

### Going Further:

* Asset
* File
* Uri
* ByteArray
* InputStream
* DocumentSource (custom)

## New Architecture: Factory vs Runtime Configuration

AndroidPDFPreview now supports a modern architecture that separates factory-time configuration from
runtime document loading. See [API Documentation](docs/API_Documentation.md)
and [Use Case Implementations](docs/Use_Case_Implementations.md) for detailed information and
examples.

### **Key Advantages:**

- **Password Retry**: Update password without reconfiguring the entire view
- **Document Switching**: Load different documents while preserving view settings
- **Compose-Friendly**: Matches AndroidView's factory/update pattern
- **Performance**: View configuration is set once and reused
- **Memory Efficient**: Only document-specific data changes during runtime

## PDF Thumbnail Generation

Generate thumbnails from PDF documents for preview purposes.
See [Use Case Implementations](docs/Use_Case_Implementations.md) for detailed examples.

## PDF Document Validation

Validate PDF documents before processing to check if they're valid, password protected, or
corrupted. See [Use Case Implementations](docs/Use_Case_Implementations.md) for detailed examples.

## Documentation

For detailed API references, use case implementations, and project information, see:

- [API Documentation](docs/API_Documentation.md) - Comprehensive guide to all parameters, methods,
  and classes.
- [Use Case Implementations](docs/Use_Case_Implementations.md) - Practical examples for common
  scenarios.
- [Changelog](docs/CHANGELOG.md) - List of changes and updates.
- [Version History](docs/VERSION_HISTORY.md) - Detailed version release notes.
- [Docs README](docs/README.md) - Additional documentation overview.

## Contributing

This library is no longer actively maintained. Pull requests will not be accepted for new features.
Only critical bug fixes for existing users may be considered on a case-by-case basis.

## Contact

For existing users with critical issues, please file an issue on GitHub. General support and feature
requests will not be addressed due to the library's maintenance status.

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
