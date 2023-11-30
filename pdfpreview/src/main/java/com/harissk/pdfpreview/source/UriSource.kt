package com.harissk.pdfpreview.source

import android.content.Context
import android.net.Uri
import com.harissk.pdfium.PdfiumCore
import java.io.IOException


/**
 * Created by Harishkumar on 25/11/23.
 */

internal class UriSource(private val uri: Uri) : DocumentSource {

    @Throws(IOException::class)
    override fun createDocument(
        context: Context,
        core: PdfiumCore,
        password: String?,
    ) = core.newDocument(context.contentResolver.openFileDescriptor(uri, "r"), password)
}