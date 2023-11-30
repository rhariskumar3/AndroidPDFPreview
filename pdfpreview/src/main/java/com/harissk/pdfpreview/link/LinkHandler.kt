package com.harissk.pdfpreview.link

import com.harissk.pdfpreview.model.LinkTapEvent

/**
 * Created by Harishkumar on 25/11/23.
 */

fun interface LinkHandler {
    /**
     * Called when link was tapped by user
     *
     * @param event current event
     */
    fun handleLinkEvent(event: LinkTapEvent)
}