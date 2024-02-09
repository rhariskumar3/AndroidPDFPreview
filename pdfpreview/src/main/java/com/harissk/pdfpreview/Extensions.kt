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