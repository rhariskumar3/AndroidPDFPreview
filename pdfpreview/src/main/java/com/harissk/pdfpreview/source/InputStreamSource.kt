package com.harissk.pdfpreview.source

import android.content.Context
import com.harissk.pdfium.PdfiumCore
import com.harissk.pdfpreview.utils.Util.toByteArray
import java.io.IOException
import java.io.InputStream

/**
 * Created by Harishkumar on 25/11/23.
 */

class InputStreamSource(private val inputStream: InputStream) : DocumentSource {

    @Throws(IOException::class)
    override fun createDocument(
        context: Context,
        core: PdfiumCore,
        password: String?,
    ) = core.newDocument(toByteArray(inputStream), password)
}