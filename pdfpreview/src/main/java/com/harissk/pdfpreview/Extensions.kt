package com.harissk.pdfpreview

import android.net.Uri
import com.harissk.pdfpreview.request.PdfRequest
import com.harissk.pdfpreview.source.AssetSource
import com.harissk.pdfpreview.source.ByteArraySource
import com.harissk.pdfpreview.source.DocumentSource
import com.harissk.pdfpreview.source.FileSource
import com.harissk.pdfpreview.source.InputStreamSource
import com.harissk.pdfpreview.source.UriSource
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
fun PDFView.load(
    assetName: String,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(AssetSource(assetName), builder)

/** Use a file as the pdf source  */
fun PDFView.load(
    file: File,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(FileSource(file), builder)

/** Use URI as the pdf source, for use with content providers  */
fun PDFView.load(
    uri: Uri,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(UriSource(uri), builder)

/** Use bytearray as the pdf source, documents is not saved  */
fun PDFView.load(
    bytes: ByteArray,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(ByteArraySource(bytes), builder)

/** Use stream as the pdf source. Stream will be written to bytearray, because native code does not support Java Streams  */
fun PDFView.load(
    stream: InputStream,
    builder: PdfRequest.Builder.() -> Unit = {},
) = load(InputStreamSource(stream), builder)