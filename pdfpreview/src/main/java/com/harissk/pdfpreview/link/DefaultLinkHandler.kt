package com.harissk.pdfpreview.link

import android.content.Intent
import android.net.Uri
import com.harissk.pdfpreview.PDFView
import com.harissk.pdfpreview.model.LinkTapEvent

/**
 * A default implementation of the {@link LinkHandler} interface that handles
 * link taps by opening URIs or jumping to internal page destinations.
 *
 * @param pdfView The {@link PDFView} that the handler is associated with.
 */
class DefaultLinkHandler(private val pdfView: PDFView) : LinkHandler {

    /**
     * Handles the link tap event by either opening the URI of the link or jumping to the
     * destination page, if applicable.
     *
     * @param event The current link tap event.
     */
    override fun handleLinkEvent(event: LinkTapEvent) {
        when {
            event.link.uri != null -> handleUri(event.link.uri!!)
            event.link.destPageIdx != null -> pdfView.jumpTo(event.link.destPageIdx!!)
        }
    }

    /**
     * Opens a link with a given URI.
     *
     * @param uri The URI to open.
     */
    private fun handleUri(uri: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        if (intent.resolveActivity(pdfView.context.packageManager) == null) return
        pdfView.context.startActivity(intent)
    }
}