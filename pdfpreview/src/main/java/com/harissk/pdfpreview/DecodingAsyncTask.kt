package com.harissk.pdfpreview

import android.os.AsyncTask
import com.harissk.pdfpreview.source.DocumentSource
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import com.shockwave.pdfium.util.Size
import java.lang.ref.WeakReference


/**
 * Created by Harishkumar on 25/11/23.
 */

internal class DecodingAsyncTask(
    docSource: DocumentSource,
    password: String?,
    userPages: IntArray?,
    pdfView: PDFView,
    pdfiumCore: PdfiumCore,
) :
    AsyncTask<Void?, Void?, Throwable?>() {
    private var cancelled: Boolean
    private val pdfViewReference: WeakReference<PDFView>
    private val pdfiumCore: PdfiumCore
    private val password: String?
    private val docSource: DocumentSource
    private val userPages: IntArray?
    private var pdfFile: PdfFile? = null

    init {
        this.docSource = docSource
        this.userPages = userPages
        cancelled = false
        pdfViewReference = WeakReference(pdfView)
        this.password = password
        this.pdfiumCore = pdfiumCore
    }

    override fun doInBackground(vararg params: Void?): Throwable? {
        return try {
            val pdfView = pdfViewReference.get()
            if (pdfView != null) {
                val pdfDocument: PdfDocument? =
                    docSource.createDocument(pdfView.context, pdfiumCore, password)
                pdfFile = PdfFile(
                    pdfiumCore = pdfiumCore,
                    pdfDocument = pdfDocument,
                    pageFitPolicy = pdfView.pageFitPolicy,
                    viewSize = getViewSize(pdfView),
                    originalUserPages = userPages,
                    isVertical = pdfView.isSwipeVertical,
                    spacing = pdfView.spacingPx,
                    autoSpacing = pdfView.isAutoSpacingEnabled,
                    fitEachPage = pdfView.isFitEachPage
                )
                null
            } else {
                NullPointerException("pdfView == null")
            }
        } catch (t: Throwable) {
            t
        }
    }

    private fun getViewSize(pdfView: PDFView): Size {
        return Size(pdfView.width, pdfView.height)
    }

    override fun onPostExecute(t: Throwable?) {
        val pdfView = pdfViewReference.get()
        if (pdfView != null) {
            if (t != null) {
                pdfView.loadError(t)
                return
            }
            if (!cancelled) {
                pdfView.loadComplete(pdfFile!!)
            }
        }
    }

    override fun onCancelled() {
        cancelled = true
    }
}