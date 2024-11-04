package com.harissk.pdfpreview.request

import com.harissk.pdfium.listener.LogWriter
import com.harissk.pdfpreview.link.LinkHandler
import com.harissk.pdfpreview.listener.DocumentLoadListener
import com.harissk.pdfpreview.listener.GestureEventListener
import com.harissk.pdfpreview.listener.PageNavigationEventListener
import com.harissk.pdfpreview.listener.RenderingEventListener
import com.harissk.pdfpreview.scroll.ScrollHandle
import com.harissk.pdfpreview.source.DocumentSource
import com.harissk.pdfpreview.utils.FitPolicy

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
 * Represents a configuration request for loading and rendering a PDF document.
 */
data class PdfRequest(
    val source: DocumentSource,
    val pageNumbers: List<Int>? = null,
    val enableSwipe: Boolean = true,
    val enableDoubleTap: Boolean = true,
    val defaultPage: Int = 0,
    val swipeHorizontal: Boolean = false,
    val annotationRendering: Boolean = false,
    val password: String? = null,
    val scrollHandle: ScrollHandle? = null,
    val antialiasing: Boolean = true,
    val spacing: Float = 0F,
    val autoSpacing: Boolean = false,
    val pageFitPolicy: FitPolicy = FitPolicy.WIDTH,
    val fitEachPage: Boolean = false,
    val pageFling: Boolean = false,
    val pageSnap: Boolean = false,
    val nightMode: Boolean = false,
    val disableLongPress: Boolean = false,
    val renderOptions: RenderOptions = RenderOptions.DEFAULT,
    val documentLoadListener: DocumentLoadListener? = null,
    val renderingEventListener: RenderingEventListener? = null,
    val pageNavigationEventListener: PageNavigationEventListener? = null,
    val gestureEventListener: GestureEventListener? = null,
    val linkHandler: LinkHandler? = null,
    val logWriter: LogWriter? = null,
) {
    class Builder(private val source: DocumentSource) {
        private var pageNumbers: List<Int>? = null
        private var enableSwipe: Boolean = true
        private var enableDoubleTap: Boolean = true
        private var defaultPage: Int = 0
        private var swipeHorizontal: Boolean = false
        private var annotationRendering: Boolean = false
        private var password: String? = null
        private var scrollHandle: ScrollHandle? = null
        private var antialiasing: Boolean = true
        private var spacing: Float = 0F
        private var autoSpacing: Boolean = false
        private var pageFitPolicy: FitPolicy = FitPolicy.WIDTH
        private var fitEachPage: Boolean = false
        private var pageFling: Boolean = false
        private var pageSnap: Boolean = false
        private var nightMode: Boolean = false
        private var disableLongPress: Boolean = false
        private var renderOptions: RenderOptions = RenderOptions.DEFAULT
        private var documentLoadListener: DocumentLoadListener? = null
        private var renderingEventListener: RenderingEventListener? = null
        private var pageNavigationEventListener: PageNavigationEventListener? = null
        private var gestureEventListener: GestureEventListener? = null
        private var linkHandler: LinkHandler? = null
        private var logWriter: LogWriter? = null

        fun pages(vararg pageNumbers: Int): Builder {
            this.pageNumbers = pageNumbers.toList()
            return this
        }

        fun enableSwipe(enableSwipe: Boolean): Builder {
            this.enableSwipe = enableSwipe
            return this
        }

        fun enableDoubleTap(doubleTap: Boolean): Builder {
            this.enableDoubleTap = doubleTap
            return this
        }

        fun enableAnnotationRendering(annotationRendering: Boolean): Builder {
            this.annotationRendering = annotationRendering
            return this
        }

        fun defaultPage(defaultPage: Int): Builder {
            this.defaultPage = defaultPage
            return this
        }

        fun swipeHorizontal(swipeHorizontal: Boolean): Builder {
            this.swipeHorizontal = swipeHorizontal
            return this
        }

        fun password(password: String?): Builder {
            this.password = password
            return this
        }

        fun scrollHandle(scrollHandle: ScrollHandle?): Builder {
            this.scrollHandle = scrollHandle
            return this
        }

        fun enableAntialiasing(antialiasing: Boolean): Builder {
            this.antialiasing = antialiasing
            return this
        }

        fun spacing(spacing: Float): Builder {
            this.spacing = spacing
            return this
        }

        fun autoSpacing(autoSpacing: Boolean): Builder {
            this.autoSpacing = autoSpacing
            return this
        }

        fun pageFitPolicy(pageFitPolicy: FitPolicy): Builder {
            this.pageFitPolicy = pageFitPolicy
            return this
        }

        fun fitEachPage(fitEachPage: Boolean): Builder {
            this.fitEachPage = fitEachPage
            return this
        }

        fun pageSnap(pageSnap: Boolean): Builder {
            this.pageSnap = pageSnap
            return this
        }

        fun pageFling(pageFling: Boolean): Builder {
            this.pageFling = pageFling
            return this
        }

        fun nightMode(nightMode: Boolean): Builder {
            this.nightMode = nightMode
            return this
        }

        fun disableLongPress(): Builder {
            this.disableLongPress = true
            return this
        }

        fun renderOptions(renderOptions: RenderOptions): Builder {
            this.renderOptions = renderOptions
            return this
        }

        fun documentLoadListener(documentLoadListener: DocumentLoadListener): Builder {
            this.documentLoadListener = documentLoadListener
            return this
        }

        fun renderingEventListener(renderingEventListener: RenderingEventListener): Builder {
            this.renderingEventListener = renderingEventListener
            return this
        }

        fun pageNavigationEventListener(
            pageNavigationEventListener: PageNavigationEventListener,
        ): Builder {
            this.pageNavigationEventListener = pageNavigationEventListener
            return this
        }

        fun gestureEventListener(gestureEventListener: GestureEventListener): Builder {
            this.gestureEventListener = gestureEventListener
            return this
        }

        fun linkHandler(linkHandler: LinkHandler): Builder {
            this.linkHandler = linkHandler
            return this
        }

        fun logWriter(logWriter: LogWriter): Builder {
            this.logWriter = logWriter
            return this
        }

        /**
         * Create a new [PdfRequest].
         */
        fun build() = PdfRequest(
            source = source,
            pageNumbers = pageNumbers,
            enableSwipe = enableSwipe,
            enableDoubleTap = enableDoubleTap,
            defaultPage = defaultPage,
            swipeHorizontal = swipeHorizontal,
            annotationRendering = annotationRendering,
            password = password,
            scrollHandle = scrollHandle,
            antialiasing = antialiasing,
            spacing = spacing,
            autoSpacing = autoSpacing,
            pageFitPolicy = pageFitPolicy,
            fitEachPage = fitEachPage,
            pageSnap = pageSnap,
            pageFling = pageFling,
            nightMode = nightMode,
            disableLongPress = disableLongPress,
            renderOptions = renderOptions,
            documentLoadListener = documentLoadListener,
            renderingEventListener = renderingEventListener,
            pageNavigationEventListener = pageNavigationEventListener,
            gestureEventListener = gestureEventListener,
            linkHandler = linkHandler,
            logWriter = logWriter,
        )
    }

    override fun toString(): String = source.toString()
}