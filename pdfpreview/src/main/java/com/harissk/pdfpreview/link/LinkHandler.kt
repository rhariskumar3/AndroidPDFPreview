package com.harissk.pdfpreview.link

import com.harissk.pdfpreview.model.LinkTapEvent

/**
 * An interface representing a handler for link tap events in a PDF previewer.
 */
interface LinkHandler {
    /**
     * Called when a link is tapped by the user.
     *
     * @param event The current link tap event.
     */
    fun handleLinkEvent(event: LinkTapEvent)
}