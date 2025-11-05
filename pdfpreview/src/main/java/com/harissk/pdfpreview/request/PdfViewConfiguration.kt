package com.harissk.pdfpreview.request

import com.harissk.pdfium.listener.LogWriter
import com.harissk.pdfpreview.link.LinkHandler
import com.harissk.pdfpreview.listener.GestureEventListener
import com.harissk.pdfpreview.listener.PageNavigationEventListener
import com.harissk.pdfpreview.listener.RenderingEventListener
import com.harissk.pdfpreview.listener.ZoomEventListener
import com.harissk.pdfpreview.scroll.ScrollHandle
import com.harissk.pdfpreview.utils.FitPolicy

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
 * Configuration for PDF view behavior and rendering settings.
 * These settings are typically set once when the view is created (factory time)
 * and remain consistent across different document loads.
 *
 * @property enableSwipeNavigation Enables or disables swipe gestures for page navigation. Defaults to true.
 * @property enableDoubleTapZoom Enables or disables double tap gestures for zooming. Defaults to true.
 * @property horizontalSwipeNavigation If true, swipe gestures will navigate horizontally instead of vertically. Defaults to false.
 * @property enableAnnotationRendering Enables or disables rendering of PDF annotations. Defaults to false.
 * @property enableAntialiasing Enables or disables anti-aliasing, which smooths jagged edges in rendered text and graphics by blending edge pixels, resulting in cleaner, more professional-looking content at the cost of slightly higher rendering time. Defaults to true.
 * @property pageSpacingDp The spacing between pages in dp. Defaults to 0F.
 * @property automaticPageSpacing If true, automatically adjusts spacing between pages based on screen size. Defaults to false.
 * @property pageFitPolicy The policy to use for fitting the page content to the screen. Defaults to [FitPolicy.WIDTH].
 * @property fitEachPageIndividually If true, each page will be individually fitted to the screen. Defaults to false.
 * @property enablePageFling Enables or disables inertial scrolling where swipes continue moving with momentum after finger lift, like throwing a page. Provides faster navigation through momentum physics. Defaults to false.
 * @property enablePageSnapping Enables or disables automatic page alignment where scrolling stops at page boundaries, ensuring pages are fully visible without partial overlaps. Defaults to false.
 * @property enableScrollOptimization Enables or disables scroll performance optimization that skips bitmap rendering during active scrolling to improve responsiveness, potentially showing blank areas that fill in after scrolling stops. Defaults to true.
 * @property enableNightMode Enables or disables night mode, which inverts the colors for better readability in low-light conditions. Defaults to false.
 * @property disableLongPressGestures Disables long press gestures on the PDF view. Defaults to false.
 * @property scrollHandle The type of scroll handle to display. Defaults to null (no scroll handle).
 * @property pdfViewerConfiguration Custom rendering options for the PDF document. Defaults to [PdfViewerConfiguration.DEFAULT].
 * @property renderingEventListener A listener to be notified of rendering events (e.g., when a page is rendered). Defaults to null.
 * @property pageNavigationEventListener A listener to be notified of page navigation events (e.g., when a page is changed). Defaults to null.
 * @property zoomEventListener A listener to be notified of zoom level changes (e.g., when zoom gesture completes). Defaults to null.
 * @property gestureEventListener A listener to be notified of gesture events (e.g., when a gesture is detected). Defaults to null.
 * @property linkHandler A handler for processing link clicks in the PDF document. Defaults to null.
 * @property logWriter A writer for logging messages and errors. Defaults to null.
 * @property enableSinglePageMode When true, displays exactly one page at a time with no adjacent page visibility, like a traditional document viewer. When false, allows continuous scrolling with multiple pages visible simultaneously. Defaults to false.
 */
data class PdfViewConfiguration(
    val enableSwipeNavigation: Boolean = true,
    val enableDoubleTapZoom: Boolean = true,
    val horizontalSwipeNavigation: Boolean = false,
    val enableAnnotationRendering: Boolean = false,
    val enableAntialiasing: Boolean = true,
    val pageSpacingDp: Float = 0F,
    val automaticPageSpacing: Boolean = false,
    val pageFitPolicy: FitPolicy = FitPolicy.WIDTH,
    val fitEachPageIndividually: Boolean = false,
    val enablePageFling: Boolean = false,
    val enablePageSnapping: Boolean = false,
    val enableScrollOptimization: Boolean = true,
    val enableNightMode: Boolean = false,
    val disableLongPressGestures: Boolean = false,
    val scrollHandle: ScrollHandle? = null,
    val pdfViewerConfiguration: PdfViewerConfiguration = PdfViewerConfiguration.DEFAULT,
    val renderingEventListener: RenderingEventListener? = null,
    val pageNavigationEventListener: PageNavigationEventListener? = null,
    val zoomEventListener: ZoomEventListener? = null,
    val gestureEventListener: GestureEventListener? = null,
    val linkHandler: LinkHandler? = null,
    val logWriter: LogWriter? = null,
    val enableSinglePageMode: Boolean = false,
) {

    class Builder {
        private var enableSwipeNavigation: Boolean = true
        private var enableDoubleTapZoom: Boolean = true
        private var horizontalSwipeNavigation: Boolean = false
        private var enableAnnotationRendering: Boolean = false
        private var enableAntialiasing: Boolean = true
        private var pageSpacingDp: Float = 0F
        private var automaticPageSpacing: Boolean = false
        private var pageFitPolicy: FitPolicy = FitPolicy.WIDTH
        private var fitEachPageIndividually: Boolean = false
        private var enablePageFling: Boolean = false
        private var enablePageSnapping: Boolean = false
        private var enableScrollOptimization: Boolean = true
        private var enableNightMode: Boolean = false
        private var disableLongPressGestures: Boolean = false
        private var scrollHandle: ScrollHandle? = null
        private var pdfViewerConfiguration: PdfViewerConfiguration = PdfViewerConfiguration.DEFAULT
        private var renderingEventListener: RenderingEventListener? = null
        private var pageNavigationEventListener: PageNavigationEventListener? = null
        private var zoomEventListener: ZoomEventListener? = null
        private var gestureEventListener: GestureEventListener? = null
        private var linkHandler: LinkHandler? = null
        private var logWriter: LogWriter? = null
        private var enableSinglePageMode: Boolean = false

        fun enableSwipeNavigation(enableSwipeNavigation: Boolean): Builder {
            this.enableSwipeNavigation = enableSwipeNavigation
            return this
        }

        fun enableDoubleTapZoom(doubleTapZoom: Boolean): Builder {
            this.enableDoubleTapZoom = doubleTapZoom
            return this
        }

        fun horizontalSwipeNavigation(horizontalSwipeNavigation: Boolean): Builder {
            this.horizontalSwipeNavigation = horizontalSwipeNavigation
            return this
        }

        fun enableAnnotationRendering(annotationRendering: Boolean): Builder {
            this.enableAnnotationRendering = annotationRendering
            return this
        }

        fun enableAntialiasing(antialiasing: Boolean): Builder {
            this.enableAntialiasing = antialiasing
            return this
        }

        fun pageSpacingDp(pageSpacingDp: Float): Builder {
            this.pageSpacingDp = pageSpacingDp
            return this
        }

        fun automaticPageSpacing(automaticPageSpacing: Boolean): Builder {
            this.automaticPageSpacing = automaticPageSpacing
            return this
        }

        fun pageFitPolicy(pageFitPolicy: FitPolicy): Builder {
            this.pageFitPolicy = pageFitPolicy
            return this
        }

        fun fitEachPageIndividually(fitEachPageIndividually: Boolean): Builder {
            this.fitEachPageIndividually = fitEachPageIndividually
            return this
        }

        fun enablePageFling(enablePageFling: Boolean): Builder {
            this.enablePageFling = enablePageFling
            return this
        }

        fun enablePageSnapping(enablePageSnapping: Boolean): Builder {
            this.enablePageSnapping = enablePageSnapping
            return this
        }

        fun enableScrollOptimization(enableScrollOptimization: Boolean): Builder {
            this.enableScrollOptimization = enableScrollOptimization
            return this
        }

        fun enableNightMode(enableNightMode: Boolean): Builder {
            this.enableNightMode = enableNightMode
            return this
        }

        fun disableLongPressGestures(): Builder {
            this.disableLongPressGestures = true
            return this
        }

        fun scrollHandle(scrollHandle: ScrollHandle?): Builder {
            this.scrollHandle = scrollHandle
            return this
        }

        fun renderOptions(pdfViewerConfiguration: PdfViewerConfiguration): Builder {
            this.pdfViewerConfiguration = pdfViewerConfiguration
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

        fun zoomEventListener(zoomEventListener: ZoomEventListener): Builder {
            this.zoomEventListener = zoomEventListener
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

        fun enableSinglePageMode(enableSinglePageMode: Boolean): Builder {
            this.enableSinglePageMode = enableSinglePageMode
            return this
        }

        fun build() = PdfViewConfiguration(
            enableSwipeNavigation = enableSwipeNavigation,
            enableDoubleTapZoom = enableDoubleTapZoom,
            horizontalSwipeNavigation = horizontalSwipeNavigation,
            enableAnnotationRendering = enableAnnotationRendering,
            enableAntialiasing = enableAntialiasing,
            pageSpacingDp = pageSpacingDp,
            automaticPageSpacing = automaticPageSpacing,
            pageFitPolicy = pageFitPolicy,
            fitEachPageIndividually = fitEachPageIndividually,
            enablePageFling = enablePageFling,
            enablePageSnapping = enablePageSnapping,
            enableScrollOptimization = enableScrollOptimization,
            enableNightMode = enableNightMode,
            disableLongPressGestures = disableLongPressGestures,
            scrollHandle = scrollHandle,
            pdfViewerConfiguration = pdfViewerConfiguration,
            renderingEventListener = renderingEventListener,
            pageNavigationEventListener = pageNavigationEventListener,
            zoomEventListener = zoomEventListener,
            gestureEventListener = gestureEventListener,
            linkHandler = linkHandler,
            logWriter = logWriter,
            enableSinglePageMode = enableSinglePageMode,
        )
    }

    companion object {
        val DEFAULT = PdfViewConfiguration()
    }
}