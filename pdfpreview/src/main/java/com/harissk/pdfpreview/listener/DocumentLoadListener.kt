package com.harissk.pdfpreview.listener

import androidx.annotation.MainThread

/**
 * Created by Harishkumar on 05/12/23.
 */
/**
 * For PDF Loading Events
 */
interface DocumentLoadListener {

    /**
     * Called when the PDF loading process starts
     */
    @MainThread
    fun onDocumentLoadingStart()

    /**
     * Called when the PDF is loaded
     * @param totalPages the number of pages in this PDF file
     */
    @MainThread
    fun onDocumentLoaded(totalPages: Int)

    /**
     * Called if error occurred while opening PDF
     * @param error Throwable with error
     */
    @MainThread
    fun onDocumentLoadError(error: Throwable)
}