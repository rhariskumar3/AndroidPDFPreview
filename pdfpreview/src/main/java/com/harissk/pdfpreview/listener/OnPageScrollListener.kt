package com.harissk.pdfpreview.listener

/**
 * Created by Harishkumar on 25/11/23.
 */
/**
 * Implements this interface to receive events from PDFView
 * when a page has been scrolled
 */
fun interface OnPageScrollListener {
    /**
     * Called on every move while scrolling
     *
     * @param page current page index
     * @param positionOffset see [com.harissk.pdfpreview.PDFView.getPositionOffset]
     */
    fun onPageScrolled(page: Int, positionOffset: Float)
}