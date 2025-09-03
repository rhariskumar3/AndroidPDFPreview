package com.harissk.pdfpreview.scroll

import com.harissk.pdfpreview.PDFView

/**
 * Copyright [2025] [Haris Kumar R](https://github.com/rhariskumar3)
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
 * An interface representing a scroll handle in a PDF previewer.
 */
interface ScrollHandle {
    /**
     * Used to move the handle, called internally by PDFView.
     *
     * @param position Current scroll ratio between 0 and 1.
     */
    fun setScroll(position: Float)

    /**
     * Method called by PDFView after setting the scroll handle.
     * Do not call this method manually.
     * For usage sample see [DefaultScrollHandle].
     *
     * @param pdfView The PDFView instance.
     */
    fun setupLayout(pdfView: PDFView?)

    /**
     * Method called by PDFView when the handle should be removed from layout.
     * Do not call this method manually.
     */
    fun destroyLayout()

    /**
     * Set the page number displayed on the handle.
     *
     * @param pageNum The page number.
     */
    fun setPageNum(pageNum: Int)

    /**
     * Get handle visibility.
     *
     * @return true if the handle is visible, false otherwise.
     */
    val shown: Boolean

    /**
     * Show the handle.
     */
    fun show()

    /**
     * Hide the handle immediately.
     */
    fun hide()

    /**
     * Hide the handle after some time (defined by the implementation).
     */
    fun hideDelayed()
}