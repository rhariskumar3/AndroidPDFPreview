package com.harissk.pdfpreview.source

import android.content.Context
import android.os.ParcelFileDescriptor
import com.harissk.pdfium.PdfiumCore
import java.io.File
import java.io.IOException


/**
 * Created by Harishkumar on 25/11/23.
 */

internal class AssetSource(private val assetName: String) : DocumentSource {

    @Throws(IOException::class)
    override fun createDocument(
        context: Context,
        core: PdfiumCore,
        password: String?,
    ) {
        val outFile = File(context.cacheDir, "$assetName-pdfview.pdf")
        if (assetName.contains("/")) outFile.getParentFile()?.mkdirs()
        context.assets.open(assetName).copyTo(outFile.outputStream().buffered())
        core.newDocument(
            fd = ParcelFileDescriptor.open(outFile, ParcelFileDescriptor.MODE_READ_ONLY),
            password = password
        )
    }
}