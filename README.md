<p align="center">
   <a href="https://rhariskumar3.github.io/AndroidPDFPreview/">
     <img alt="AndroidPDFPreview" src=".github/logo.png" />
   </a>
</p>

![GitHub License](https://img.shields.io/github/license/rhariskumar3/AndroidPDFPreview)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.rhariskumar3/pdfpreview)](https://central.sonatype.com/artifact/io.github.rhariskumar3/pdfpreview)
[![Kotlin](https://img.shields.io/badge/kotlin-1.8.10-orange.svg)](http://kotlinlang.org/)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/rhariskumar3/AndroidPDFPreview/android.yml)
![GitHub Issues](https://img.shields.io/github/issues/rhariskumar3/AndroidPDFPreview)

AndroidPDFPreview is a lightweight and easy-to-use library for displaying and interacting with PDFs
in your Android applications. Built on top of PdfiumAndroid for decoding and AndroidPdfViewer for
rendering, it delivers a smooth and user-friendly experience.

## Features

* Display and interact with PDF documents
* Support for gestures, zoom, and double tap
* Lightweight and easy to integrate into your Android apps
* Compatible with Android versions 5.1 and above

## Installation

To install AndroidPDFPreview, add the following dependency to your project's Gradle file:

```
dependencies {
    implementation 'io.github.rhariskumar3:pdfpreview:1.0.5'
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

### Going Further:

* Asset
* File
* Uri
* ByteArray
* InputStream
* DocumentSource (custom)

## Additional Features

AndroidPDFPreview supports a number of additional features, including:

* Page navigation
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

    Copyright 2024 AndroidPDFPreview Contributors

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
