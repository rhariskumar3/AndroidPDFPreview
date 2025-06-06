package com.harissk.pdfpreview

import android.net.Uri
import com.harissk.pdfpreview.request.PdfRequest
import com.harissk.pdfpreview.source.DocumentSource
import java.io.File
import java.io.InputStream

/**
 * Copyright [2024] [Haris Kumar R](https://github.com/rhariskumar3)
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
 * @param builder A lambda function to configure the PDF request using a {@link PdfRequest.Builder}
 * instance.
 *
 * @see [DocumentSource]
 * @see [PdfRequest.Builder]
 */
inline fun PDFView.load(
    source: DocumentSource,
    builder: PdfRequest.Builder.() -> Unit = {},
) {
    enqueue(
        pdfRequest = PdfRequest.Builder(source)
            .apply(builder)
            .build()
    )
}

/**
 * Load PDF from the assets file.
 *
 * @param assetName The name of the asset file containing the PDF document.
 * @param builder A lambda function to configure the PDF request using a {@link PdfRequest.Builder}
 * instance.
 *
 * @see [PdfRequest.Builder]
 */
inline fun PDFView.load(
    assetName: String,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(DocumentSource.toDocumentSource(assetName), builder)

/**
 * Load PDF from a file.
 *
 * @param file     The {@link File} containing the PDF document.
 * @param builder A lambda function to configure the PDF request using a {@link PdfRequest.Builder}
 * instance.
 *
 * @see [PdfRequest.Builder]
 */
inline fun PDFView.load(
    file: File,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(DocumentSource.toDocumentSource(file), builder)

/**
 * Load PDF from a URI. Useful for content providers.
 *
 * @param uri      The {@link Uri} pointing to the PDF document.
 * @param builder A lambda function to configure the PDF request using a {@link PdfRequest.Builder}
 * instance.
 *
 * @see [PdfRequest.Builder]
 */
inline fun PDFView.load(
    uri: Uri,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(DocumentSource.toDocumentSource(uri), builder)

/**
 * Load PDF from a byte array. Document is not saved on disk.
 *
 * @param bytes   The byte array containing the PDF document content.
 * @param builder A lambda function to configure the PDF request using a {@link PdfRequest.Builder}
 * instance.
 *
 * @see [PdfRequest.Builder]
 */
inline fun PDFView.load(
    bytes: ByteArray,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(DocumentSource.toDocumentSource(bytes), builder)

/**
 * Load PDF from an {@link InputStream}. The stream is converted to a byte array, since native
 * code does not support direct stream handling.
 *
 * @param stream The {@link InputStream} containing the PDF document content.
 * @param builder A lambda function to configure the PDF request using a {@link PdfRequest.Builder}
 * instance.
 *
 * @see [PdfRequest.Builder]
 */
inline fun PDFView.load(
    stream: InputStream,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(DocumentSource.toDocumentSource(stream), builder)