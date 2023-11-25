package com.harissk.pdfpreview.source

import android.content.Context
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.IOException


/**
 * Created by Harishkumar on 25/11/23.
 */

fun interface DocumentSource {
    @Throws(IOException::class)
    fun createDocument(context: Context, core: PdfiumCore, password: String?): PdfDocument?
}