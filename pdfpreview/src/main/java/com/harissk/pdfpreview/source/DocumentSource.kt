package com.harissk.pdfpreview.source

import android.content.Context
import android.net.Uri
import com.harissk.pdfium.PdfiumCore
import java.io.File
import java.io.IOException
import java.io.InputStream


/**
 * Created by Harishkumar on 25/11/23.
 */

/**
 * A functional interface representing a source for creating a PDF document.
 * Implementations handle opening and loading the document from various sources.
 */
fun interface DocumentSource {

    /**
     * Creates a PDF document from this source.
     *
     * @param context The application context.
     * @param core The PdfiumCore instance for document handling.
     * @param password The password to open the document, if required.
     *
     * @throws IOException If an error occurs during document creation.
     */
    @Throws(IOException::class)
    fun createDocument(context: Context, core: PdfiumCore, password: String?)

    companion object {

        /**
         * Converts a supported object to its corresponding [DocumentSource] instance.
         *
         * @param source The object to convert.
         * @return The appropriate [DocumentSource] instance for the given object.
         * @throws IllegalArgumentException If the provided object is not a supported source type.
         */
        fun toDocumentSource(source: Any): DocumentSource = when (source) {
            is String -> AssetSource(source)  // Assumes string represents an asset path
            is File -> FileSource(source)
            is Uri -> UriSource(source)
            is ByteArray -> ByteArraySource(source)
            is InputStream -> InputStreamSource(source)
            is DocumentSource -> source
            else -> throw IllegalArgumentException("Unsupported document source type: $source")
        }
    }
}
