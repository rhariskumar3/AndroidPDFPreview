package com.harissk.pdfpreview.listener

import android.graphics.Canvas
import androidx.annotation.MainThread

import com.harissk.pdfpreview.exception.PageRenderingException

/**
 * Copyright [2024] [Haris Kumar R](https://github.com/rhariskumar3)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * */

/**
 * Interface for listening to rendering events in a PDF previewer.
 */
interface RenderingEventListener {
    /**
     * Called when a specific page in the PDF document has been rendered.
     *
     * @param pageNumber The page number that has been rendered.
     */
    @MainThread
    fun onPageRendered(pageNumber: Int)

    /**
     * Called when a specific page in the PDF document has failed to render.
     *
     * @param pageRenderingException The {@link PageRenderingException} that caused the page to fail to
     * render.
     */
    @MainThread
    fun onPageFailedToRender(pageRenderingException: PageRenderingException)

    /**
     * This method is called when the PDFView is drawing its view.
     * <p>
     * The page is starting at (0,0).
     *
     * @param canvas        The canvas on which to draw things.
     * @param pageWidth     The width of the current page.
     * @param pageHeight    The height of the current page.
     * @param displayedPage The current page index.
     */
    @MainThread
    fun onDrawPage(canvas: Canvas?, pageWidth: Float, pageHeight: Float, displayedPage: Int)
}