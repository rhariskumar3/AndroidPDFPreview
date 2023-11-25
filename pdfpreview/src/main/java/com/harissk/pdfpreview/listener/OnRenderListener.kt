package com.harissk.pdfpreview.listener

/**
 * Created by Harishkumar on 25/11/23.
 */

fun interface OnRenderListener {
    /**
     * Called only once, when document is rendered
     * @param pages number of pages
     */
    fun onInitiallyRendered(pages: Int)
}