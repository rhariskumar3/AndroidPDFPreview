package com.harissk.pdfpreview.request

import com.harissk.pdfpreview.link.LinkHandler
import com.harissk.pdfpreview.listener.OnDrawListener
import com.harissk.pdfpreview.listener.OnErrorListener
import com.harissk.pdfpreview.listener.OnLoadCompleteListener
import com.harissk.pdfpreview.listener.OnLongPressListener
import com.harissk.pdfpreview.listener.OnPageChangeListener
import com.harissk.pdfpreview.listener.OnPageErrorListener
import com.harissk.pdfpreview.listener.OnPageScrollListener
import com.harissk.pdfpreview.listener.OnRenderListener
import com.harissk.pdfpreview.listener.OnTapListener
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
    val onDrawListener: OnDrawListener? = null,
    val onDrawAllListener: OnDrawListener? = null,
    val onLoadCompleteListener: OnLoadCompleteListener? = null,
    val onErrorListener: OnErrorListener? = null,
    val onPageChangeListener: OnPageChangeListener? = null,
    val onPageScrollListener: OnPageScrollListener? = null,
    val onRenderListener: OnRenderListener? = null,
    val onTapListener: OnTapListener? = null,
    val onLongPressListener: OnLongPressListener? = null,
    val onPageErrorListener: OnPageErrorListener? = null,
    val linkHandler: LinkHandler? = null,
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
) {

    class Builder {
        private var source: DocumentSource? = null
        private var pageNumbers: IntArray? = null
        private var enableSwipe: Boolean = true
        private var enableDoubleTap: Boolean = true
        private var onDrawListener: OnDrawListener? = null
        private var onDrawAllListener: OnDrawListener? = null
        private var onLoadCompleteListener: OnLoadCompleteListener? = null
        private var onErrorListener: OnErrorListener? = null
        private var onPageChangeListener: OnPageChangeListener? = null
        private var onPageScrollListener: OnPageScrollListener? = null
        private var onRenderListener: OnRenderListener? = null
        private var onTapListener: OnTapListener? = null
        private var onLongPressListener: OnLongPressListener? = null
        private var onPageErrorListener: OnPageErrorListener? = null
        private var linkHandler: LinkHandler? = null
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

        fun onDraw(onDrawListener: OnDrawListener?) = apply {
            this.onDrawListener = onDrawListener
        }

        fun onDrawAll(onDrawAllListener: OnDrawListener?) = apply {
            this.onDrawAllListener = onDrawAllListener
        }

        fun onLoad(onLoadCompleteListener: OnLoadCompleteListener?) = apply {
            this.onLoadCompleteListener = onLoadCompleteListener
        }

        fun onPageScroll(onPageScrollListener: OnPageScrollListener?) = apply {
            this.onPageScrollListener = onPageScrollListener
        }

        fun onError(onErrorListener: OnErrorListener?) = apply {
            this.onErrorListener = onErrorListener
        }

        fun onPageError(onPageErrorListener: OnPageErrorListener?) = apply {
            this.onPageErrorListener = onPageErrorListener
        }

        fun onPageChange(onPageChangeListener: OnPageChangeListener?) = apply {
            this.onPageChangeListener = onPageChangeListener
        }

        fun onRender(onRenderListener: OnRenderListener?) = apply {
            this.onRenderListener = onRenderListener
        }

        fun onTap(onTapListener: OnTapListener?) = apply {
            this.onTapListener = onTapListener
        }

        fun onLongPress(onLongPressListener: OnLongPressListener?) = apply {
            this.onLongPressListener = onLongPressListener
        }

        fun linkHandler(linkHandler: LinkHandler) = apply {
            this.linkHandler = linkHandler
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

        /**
         * Create a new [PdfRequest].
         */
        fun build() = PdfRequest(
            source = source ?: throw NullPointerException(),
            pageNumbers = pageNumbers,
            enableSwipe = enableSwipe,
            enableDoubleTap = enableDoubleTap,
            onDrawListener = onDrawListener,
            onDrawAllListener = onDrawAllListener,
            onLoadCompleteListener = onLoadCompleteListener,
            onErrorListener = onErrorListener,
            onPageChangeListener = onPageChangeListener,
            onPageScrollListener = onPageScrollListener,
            onRenderListener = onRenderListener,
            onTapListener = onTapListener,
            onLongPressListener = onLongPressListener,
            onPageErrorListener = onPageErrorListener,
            linkHandler = linkHandler,
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
            disableLongPress = disableLongPress
        )
    }
}