package com.harissk.pdfpreview.link

import android.content.Intent
import android.net.Uri
import com.harissk.pdfpreview.PDFView
import com.harissk.pdfpreview.model.LinkTapEvent


/**
 * Created by Harishkumar on 25/11/23.
 */

class DefaultLinkHandler(private val pdfView: PDFView) : LinkHandler {

    override fun handleLinkEvent(event: LinkTapEvent) {
        when {
            event.link.uri != null -> handleUri(event.link.uri!!)
            event.link.destPageIdx != null -> pdfView.jumpTo(event.link.destPageIdx!!)
        }
    }


    private fun handleUri(uri: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        if (intent.resolveActivity(pdfView.context.packageManager) == null) return
        pdfView.context.startActivity(intent)
    }
}