package com.harissk.pdfpreview.listener

import androidx.annotation.MainThread

/**
 * Created by Harishkumar on 05/12/23.
 */
/**
 * For PDF page navigation events
 */
interface PageNavigationEventListener {
    /**
     * Called when the user use swipe to change page
     *
     * @param newPage      the new page displayed, starting from 0
     * @param pageCount the total page count
     */
    @MainThread
    fun onPageChanged(newPage: Int, pageCount: Int)

    /**
     * Called on every move while scrolling
     *
     * @param page current page index
     * @param positionOffset
     */
    @MainThread
    fun onPageScrolled(page: Int, positionOffset: Float)
}