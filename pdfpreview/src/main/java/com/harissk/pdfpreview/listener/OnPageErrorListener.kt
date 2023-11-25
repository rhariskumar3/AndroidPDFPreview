package com.harissk.pdfpreview.listener

/**
 * Created by Harishkumar on 25/11/23.
 */

fun interface OnPageErrorListener {
    /**
     * Called if error occurred while loading PDF page
     * @param throwable Throwable with error
     */
    fun onPageError(page: Int, throwable: Throwable?)
}