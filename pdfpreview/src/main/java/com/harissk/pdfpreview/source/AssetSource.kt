package com.harissk.pdfpreview.source

import android.content.Context
import android.os.ParcelFileDescriptor
import com.harissk.pdfium.PdfiumCore
import com.harissk.pdfpreview.utils.FileUtils
import java.io.File
import java.io.IOException


/**
 * Created by Harishkumar on 25/11/23.
 */

class AssetSource(private val assetName: String) : DocumentSource {

    @Throws(IOException::class)
    override fun createDocument(
        context: Context,
        core: PdfiumCore,
        password: String?,
    ) {
        val f: File = FileUtils.fileFromAsset(context, assetName)
        val pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
        return core.newDocument(pfd, password)
    }
}