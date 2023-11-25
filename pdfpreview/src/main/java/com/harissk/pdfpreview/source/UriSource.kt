package com.harissk.pdfpreview.source

import android.content.Context
import android.net.Uri
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.IOException


/**
 * Created by Harishkumar on 25/11/23.
 */

class UriSource(private val uri: Uri) : DocumentSource {

    @Throws(IOException::class)
    override fun createDocument(
        context: Context,
        core: PdfiumCore,
        password: String?,
    ): PdfDocument? {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        return core.newDocument(pfd, password)
    }
}