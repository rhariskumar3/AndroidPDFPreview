package com.harissk.pdfpreview

import android.net.Uri
import com.harissk.pdfpreview.request.PdfLoadRequest
import com.harissk.pdfpreview.request.PdfViewConfiguration
import com.harissk.pdfpreview.source.DocumentSource
import java.io.File
import java.io.InputStream

/**
 * Copyright [2025] [Haris Kumar R](https://github.com/rhariskumar3)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * */
/**
 * Load PDF using custom [DocumentSource].
 *
 * @param source The {@link DocumentSource} to use for loading the PDF document.
 * @param builder A lambda function to configure the PDF load request using a {@link PdfLoadRequest.Builder}
 * instance.
 *
 * @see [DocumentSource]
 * @see [PdfLoadRequest.Builder]
 */
inline fun PDFView.loadDocument(
    source: DocumentSource,
    builder: PdfLoadRequest.Builder.() -> Unit = {},
) {
    load(
        loadRequest = PdfLoadRequest.Builder(source)
            .apply(builder)
            .build()
    )
}

/**
 * Configure PDF view with factory-time settings.
 *
 * @param builder A lambda function to configure the PDF view using a {@link PdfViewConfiguration.Builder}
 * instance.
 *
 * @see [PdfViewConfiguration.Builder]
 */
inline fun PDFView.configureView(
    builder: PdfViewConfiguration.Builder.() -> Unit = {},
) {
    configure(
        configuration = PdfViewConfiguration.Builder()
            .apply(builder)
            .build()
    )
}


/**
 * Load PDF from the assets file.
 *
 * @param assetName The name of the asset file containing the PDF document.
 * @param builder A lambda function to configure the PDF load request using a {@link PdfLoadRequest.Builder}
 * instance.
 *
 * @see [PdfLoadRequest.Builder]
 */
inline fun PDFView.loadDocument(
    assetName: String,
    builder: PdfLoadRequest.Builder.() -> Unit = {},
) = loadDocument(DocumentSource.toDocumentSource(assetName), builder)


/**
 * Load PDF from a file.
 *
 * @param file     The {@link File} containing the PDF document.
 * @param builder A lambda function to configure the PDF load request using a {@link PdfLoadRequest.Builder}
 * instance.
 *
 * @see [PdfLoadRequest.Builder]
 */
inline fun PDFView.loadDocument(
    file: File,
    builder: PdfLoadRequest.Builder.() -> Unit = {},
) = loadDocument(DocumentSource.toDocumentSource(file), builder)


/**
 * Load PDF from a URI. Useful for content providers.
 *
 * @param uri      The {@link Uri} pointing to the PDF document.
 * @param builder A lambda function to configure the PDF load request using a {@link PdfLoadRequest.Builder}
 * instance.
 *
 * @see [PdfLoadRequest.Builder]
 */
inline fun PDFView.loadDocument(
    uri: Uri,
    builder: PdfLoadRequest.Builder.() -> Unit = {},
) = loadDocument(DocumentSource.toDocumentSource(uri), builder)


/**
 * Load PDF from a byte array. Document is not saved on disk.
 *
 * @param bytes   The byte array containing the PDF document content.
 * @param builder A lambda function to configure the PDF load request using a {@link PdfLoadRequest.Builder}
 * instance.
 *
 * @see [PdfLoadRequest.Builder]
 */
inline fun PDFView.loadDocument(
    bytes: ByteArray,
    builder: PdfLoadRequest.Builder.() -> Unit = {},
) = loadDocument(DocumentSource.toDocumentSource(bytes), builder)


/**
 * Load PDF from an {@link InputStream}. The stream is converted to a byte array, since native
 * code does not support direct stream handling.
 *
 * @param stream The {@link InputStream} containing the PDF document content.
 * @param builder A lambda function to configure the PDF load request using a {@link PdfLoadRequest.Builder}
 * instance.
 *
 * @see [PdfLoadRequest.Builder]
 */
inline fun PDFView.loadDocument(
    stream: InputStream,
    builder: PdfLoadRequest.Builder.() -> Unit = {},
) = loadDocument(DocumentSource.toDocumentSource(stream), builder)

