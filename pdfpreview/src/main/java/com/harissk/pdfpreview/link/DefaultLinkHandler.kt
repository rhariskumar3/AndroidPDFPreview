package com.harissk.pdfpreview.link

import android.content.Intent
import android.net.Uri
import com.harissk.pdfpreview.PDFView
import com.harissk.pdfpreview.model.LinkTapEvent


/**
 * Created by Harishkumar on 25/11/23.
 */

class DefaultLinkHandler(private val pdfView: PDFView) : LinkHandler {

    override fun handleLinkEvent(event: LinkTapEvent?) {
        val uri = event?.link?.uri
        val page = event?.link?.destPageIdx
        if (!uri.isNullOrEmpty()) {
            handleUri(uri)
        } else page?.let { handlePage(it) }
    }

    private fun handleUri(uri: String) {
        val parsedUri = Uri.parse(uri)
        val intent = Intent(Intent.ACTION_VIEW, parsedUri)
        val context = pdfView.context
        if (intent.resolveActivity(context.packageManager) == null) return
        context.startActivity(intent)
    }

    private fun handlePage(page: Int) {
        pdfView.jumpTo(page)
    }
}