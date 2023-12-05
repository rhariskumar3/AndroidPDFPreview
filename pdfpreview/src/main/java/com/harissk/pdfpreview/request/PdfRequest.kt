package com.harissk.pdfpreview.request

import com.harissk.pdfpreview.link.LinkHandler
import com.harissk.pdfpreview.listener.DocumentLoadListener
import com.harissk.pdfpreview.listener.GestureEventListener
import com.harissk.pdfpreview.listener.PageNavigationEventListener
import com.harissk.pdfpreview.listener.RenderingEventListener
import com.harissk.pdfpreview.scroll.ScrollHandle
import com.harissk.pdfpreview.source.DocumentSource
import com.harissk.pdfpreview.utils.FitPolicy

/**
 * Created by Harishkumar on 03/12/23.
 */
/**
 * Represents a configuration request for loading and rendering a PDF document.
 */
class PdfRequest private constructor(
    val source: DocumentSource,
    val pageNumbers: IntArray? = null,
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
) {

    class Builder {
        private var source: DocumentSource? = null
        private var pageNumbers: IntArray? = null
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

        fun source(source: DocumentSource) = apply {
            this.source = source
        }

        fun pages(vararg pageNumbers: Int) = apply {
            this.pageNumbers = pageNumbers
        }

        fun enableSwipe(enableSwipe: Boolean) = apply {
            this.enableSwipe = enableSwipe
        }

        fun enableDoubleTap(doubleTap: Boolean) = apply {
            this.enableDoubleTap = doubleTap
        }

        fun enableAnnotationRendering(annotationRendering: Boolean) = apply {
            this.annotationRendering = annotationRendering
        }

        fun defaultPage(defaultPage: Int) = apply {
            this.defaultPage = defaultPage
        }

        fun swipeHorizontal(swipeHorizontal: Boolean) = apply {
            this.swipeHorizontal = swipeHorizontal
        }

        fun password(password: String?) = apply {
            this.password = password
        }

        fun scrollHandle(scrollHandle: ScrollHandle?) = apply {
            this.scrollHandle = scrollHandle
        }

        fun enableAntialiasing(antialiasing: Boolean) = apply {
            this.antialiasing = antialiasing
        }

        fun spacing(spacing: Float) = apply {
            this.spacing = spacing
        }

        fun autoSpacing(autoSpacing: Boolean) = apply {
            this.autoSpacing = autoSpacing
        }

        fun pageFitPolicy(pageFitPolicy: FitPolicy) = apply {
            this.pageFitPolicy = pageFitPolicy
        }

        fun fitEachPage(fitEachPage: Boolean) = apply {
            this.fitEachPage = fitEachPage
        }

        fun pageSnap(pageSnap: Boolean) = apply {
            this.pageSnap = pageSnap
        }

        fun pageFling(pageFling: Boolean) = apply {
            this.pageFling = pageFling
        }

        fun nightMode(nightMode: Boolean) = apply {
            this.nightMode = nightMode
        }

        fun disableLongPress() = apply {
            this.disableLongPress = true
        }

        fun renderOptions(renderOptions: RenderOptions) = apply {
            this.renderOptions = renderOptions
        }

        fun documentLoadListener(documentLoadListener: DocumentLoadListener) = apply {
            this.documentLoadListener = documentLoadListener
        }

        fun renderingEventListener(renderingEventListener: RenderingEventListener) = apply {
            this.renderingEventListener = renderingEventListener
        }

        fun pageNavigationEventListener(pageNavigationEventListener: PageNavigationEventListener) =
            apply {
                this.pageNavigationEventListener = pageNavigationEventListener
            }

        fun gestureEventListener(gestureEventListener: GestureEventListener) = apply {
            this.gestureEventListener = gestureEventListener
        }

        fun linkHandler(linkHandler: LinkHandler) = apply {
            this.linkHandler = linkHandler
        }

        /**
         * Create a new [PdfRequest].
         */
        fun build() = PdfRequest(
            source = source ?: throw NullPointerException(),
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
        )
    }
}