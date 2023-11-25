package com.harissk.pdfpreview.listener

/**
 * Created by Harishkumar on 25/11/23.
 */

fun interface OnErrorListener {
    /**
     * Called if error occurred while opening PDF
     * @param throwable Throwable with error
     */
    fun onError(throwable: Throwable?)
}