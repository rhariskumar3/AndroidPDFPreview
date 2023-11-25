# AndroidPDFPreview
AndroidPDFPreview is a lightweight and easy-to-use SDK that enables you to display and interact with PDF documents in your Android apps. Built upon PdfiumAndroid for decoding PDF files and barteksc/AndroidPdfViewer for rendering, AndroidPDFPreview provides a seamless and user-friendly experience for viewing PDFs on Android devices. With support for gestures, zoom, and double tap, AndroidPDFPreview offers a comprehensive solution for integrating PDF preview functionality into your Android apps.

## Features
* Display and interact with PDF documents
* Support for gestures, zoom, and double tap
* Lightweight and easy to integrate into your Android apps
* Compatible with Android versions 4.1 and above

## Installation
To install AndroidPDFPreview, add the following dependency to your project's Gradle file:
```
dependencies {
    implementation 'com.github.rhariskumar3:androidpdfpreview:1.0.0'
}
```

## Usage
To use AndroidPDFPreview, simply create a PDFView instance and pass it the path to the PDF file you want to display. For example:

XML
```
<com.github.rhariskumar3.androidpdfpreview.PDFView
    android:id="@+id/pdf_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:pdf_src="@raw/my_pdf.pdf" />
```
Use code with caution. Learn more

## Additional Features
AndroidPDFPreview supports a number of additional features, including:
* Page navigation
* Search
* Table of contents
* Bookmarks
* Annotations

## License
AndroidPDFPreview is licensed under the Apache License 2.0.

## Contributing
We welcome contributions to AndroidPDFPreview. Please feel free to submit pull requests or file issues on GitHub.

## Contact
If you have any questions or feedback, please feel free to contact us at [email protected]

## Acknowledgements
We would like to thank the following projects for their contributions to AndroidPDFPreview:
* [PdfiumAndroid](https://github.com/mshockwave/PdfiumAndroid)
* [barteksc/AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer)

We hope you find AndroidPDFPreview to be a valuable tool for developing your Android apps.

** Happy coding! **
