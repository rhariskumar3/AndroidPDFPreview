package com.harissk.pdfpreview

import android.net.Uri
import com.harissk.pdfpreview.request.PdfRequest
import com.harissk.pdfpreview.source.DocumentSource
import java.io.File
import java.io.InputStream

/**
 * Created by Harishkumar on 03/12/23.
 */

/** Use custom source as pdf source  */
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

/** Use an asset file as the pdf source  */
inline fun PDFView.load(
    assetName: String,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(DocumentSource.toDocumentSource(assetName), builder)

/** Use a file as the pdf source  */
inline fun PDFView.load(
    file: File,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(DocumentSource.toDocumentSource(file), builder)

/** Use URI as the pdf source, for use with content providers  */
inline fun PDFView.load(
    uri: Uri,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(DocumentSource.toDocumentSource(uri), builder)

/** Use bytearray as the pdf source, documents is not saved  */
inline fun PDFView.load(
    bytes: ByteArray,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(DocumentSource.toDocumentSource(bytes), builder)

/** Use stream as the pdf source. Stream will be written to bytearray, because native code does not support Java Streams  */
inline fun PDFView.load(
    stream: InputStream,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(DocumentSource.toDocumentSource(stream), builder)