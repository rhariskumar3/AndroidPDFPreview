package com.harissk.pdfpreview.listener

import android.graphics.Canvas
import androidx.annotation.MainThread
import com.harissk.pdfpreview.exception.PageRenderingException

/**
 * Created by Harishkumar on 05/12/23.
 */
/**
 * For PDF rendering events.
 */
interface RenderingEventListener {
    /**
     * Called when a specific page in the PDF document has been rendered.
     * @param pageNumber page number that has been rendered.
     */
    @MainThread
    fun onPageRendered(pageNumber: Int)

    /**
     * Called when a specific page in the PDF document has failed to render.
     * @param pageRenderingException exception that caused the page to fail to render.
     */
    @MainThread
    fun onPageFailedToRender(pageRenderingException: PageRenderingException)

    /**
     * This method is called when the PDFView is
     * drawing its view.
     *
     *
     * The page is starting at (0,0)
     *
     * @param canvas        The canvas on which to draw things.
     * @param pageWidth     The width of the current page.
     * @param pageHeight    The height of the current page.
     * @param displayedPage The current page index
     */
    @MainThread
    fun onDrawPage(canvas: Canvas?, pageWidth: Float, pageHeight: Float, displayedPage: Int)
}