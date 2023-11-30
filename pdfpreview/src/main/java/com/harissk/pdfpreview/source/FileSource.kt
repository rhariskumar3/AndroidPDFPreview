package com.harissk.pdfpreview.source

import android.content.Context
import android.os.ParcelFileDescriptor
import com.harissk.pdfium.PdfiumCore
import java.io.File
import java.io.IOException


/**
 * Created by Harishkumar on 25/11/23.
 */

internal class FileSource(private val file: File) : DocumentSource {

    @Throws(IOException::class)
    override fun createDocument(
        context: Context,
        core: PdfiumCore,
        password: String?,
    ) = core.newDocument(
        fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY),
        password = password
    )
}