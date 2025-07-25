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
 *
 * This class encapsulates all the settings and options that can be applied when loading and displaying a PDF.
 * It's used to customize the behavior of the PDF viewer, such as enabling/disabling features,
 * setting the initial page, defining the page fit policy, and more.
 *
 * @property source The source of the PDF document to load (e.g., file, asset, URI).
 * @property pageNumbers An optional list of specific page numbers to load. If null, all pages are loaded.
 * @property enableSwipe Enables or disables swipe gestures for page navigation. Defaults to true.
 * @property enableDoubleTap Enables or disables double tap gestures for zooming. Defaults to true.
 * @property defaultPage The initial page number to display when the document is loaded. Defaults to 0 (the first page).
 * @property swipeHorizontal If true, swipe gestures will navigate horizontally instead of vertically. Defaults to false.
 * @property annotationRendering Enables or disables rendering of PDF annotations. Defaults to false.
 * @property password The password to use if the PDF document is encrypted. Defaults to null.
 * @property scrollHandle The type of scroll handle to display. Defaults to null (no scroll handle).
 * @property antialiasing Enables or disables anti-aliasing for smoother rendering. Defaults to true.
 * @property spacing The spacing between pages in pixels. Defaults to 0F.
 * @property autoSpacing If true, automatically adjusts spacing between pages based on screen size. Defaults to false.
 * @property pageFitPolicy The policy to use for fitting the page content to the screen. Defaults to [FitPolicy.WIDTH].
 * @property fitEachPage If true, each page will be individually fitted to the screen. Defaults to false.
 * @property pageFling Enables or disables page flinging for faster navigation. Defaults to false.
 * @property pageSnap Enables or disables page snapping, where pages will snap to the screen edges. Defaults to false.
 * @property scrollOptimization Enables or disables scroll optimization. When true, bitmap generation is skipped during scrolling for better performance, but may show empty areas when scrolling to new content. Defaults to true.
 * @property nightMode Enables or disables night mode, which inverts the colors for better readability in low-light conditions. Defaults to false.
 * @property disableLongPress Disables long press gestures on the PDF view. Defaults to false.
 * @property pdfViewerConfiguration Custom rendering options for the PDF document. Defaults to [PdfViewerConfiguration.DEFAULT].
 * @property documentLoadListener A listener to be notified when the document is loaded and ready for rendering. Defaults to null.
 * @property renderingEventListener A listener to be notified of rendering events (e.g., when a page is rendered). Defaults to null.
 * @property pageNavigationEventListener A listener to be notified of page navigation events (e.g., when a page is changed). Defaults to null.
 * @property gestureEventListener A listener to be notified of gesture events (e.g., when a gesture is detected). Defaults to null.
 * @property linkHandler A handler for processing link clicks in the PDF document. Defaults to null.
 * @property logWriter A writer for logging messages and errors. Defaults to null.
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
    val scrollOptimization: Boolean = true,
    val nightMode: Boolean = false,
    val disableLongPress: Boolean = false,
    val pdfViewerConfiguration: PdfViewerConfiguration = PdfViewerConfiguration.DEFAULT,
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
        private var scrollOptimization: Boolean = true
        private var nightMode: Boolean = false
        private var disableLongPress: Boolean = false
        private var pdfViewerConfiguration: PdfViewerConfiguration = PdfViewerConfiguration.DEFAULT
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

        fun scrollOptimization(scrollOptimization: Boolean): Builder {
            this.scrollOptimization = scrollOptimization
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

        fun renderOptions(pdfViewerConfiguration: PdfViewerConfiguration): Builder {
            this.pdfViewerConfiguration = pdfViewerConfiguration
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
            scrollOptimization = scrollOptimization,
            nightMode = nightMode,
            disableLongPress = disableLongPress,
            pdfViewerConfiguration = pdfViewerConfiguration,
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