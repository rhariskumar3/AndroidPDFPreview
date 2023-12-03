package com.harissk.pdfpreview

import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.View
import android.view.View.OnTouchListener
import com.harissk.pdfium.util.SizeF
import com.harissk.pdfpreview.model.LinkTapEvent
import com.harissk.pdfpreview.utils.Constants.Pinch.MAXIMUM_ZOOM
import com.harissk.pdfpreview.utils.Constants.Pinch.MINIMUM_ZOOM
import com.harissk.pdfpreview.utils.SnapEdge
import kotlin.math.abs


/**
 * Created by Harishkumar on 25/11/23.
 */
/**
 * This Manager takes care of moving the PDFView,
 * set its zoom track user actions.
 */
internal class DragPinchManager(private val pdfView: PDFView, animationManager: AnimationManager) :
    GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, OnScaleGestureListener,
    OnTouchListener {

    private val animationManager: AnimationManager
    private val gestureDetector: GestureDetector
    private val scaleGestureDetector: ScaleGestureDetector
    private var scrolling = false
    private var scaling = false
    private var enabled = false

    init {
        this.animationManager = animationManager
        gestureDetector = GestureDetector(pdfView.context, this)
        scaleGestureDetector = ScaleGestureDetector(pdfView.context, this)
        pdfView.setOnTouchListener(this)
    }

    fun enable() {
        enabled = true
    }

    fun disable() {
        enabled = false
    }

    fun disableLongPress() {
        gestureDetector.setIsLongpressEnabled(false)
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        val onTapHandled: Boolean = pdfView.callbacks.callOnTap(e)
        val linkTapped = checkLinkTapped(e.x, e.y)
        if (!onTapHandled && !linkTapped) {
            if (!pdfView.documentFitsView()) {
                when {
                    pdfView.scrollHandle?.shown == true -> pdfView.scrollHandle?.hide()
                    else -> pdfView.scrollHandle?.show()
                }
            }
        }
        pdfView.performClick()
        return true
    }

    private fun checkLinkTapped(x: Float, y: Float): Boolean {
        val pdfFile: PdfFile = pdfView.pdfFile ?: return false
        val mappedX: Float = -pdfView.currentXOffset + x
        val mappedY: Float = -pdfView.currentYOffset + y
        val page = pdfFile.getPageAtOffset(
            if (pdfView.isSwipeVertical) mappedY else mappedX,
            pdfView.zoom
        )
        val pageSize: SizeF = pdfFile.getScaledPageSize(page, pdfView.zoom)
        val pageX: Int
        val pageY: Int
        if (pdfView.isSwipeVertical) {
            pageX = pdfFile.getSecondaryPageOffset(page, pdfView.zoom).toInt()
            pageY = pdfFile.getPageOffset(page, pdfView.zoom).toInt()
        } else {
            pageY = pdfFile.getSecondaryPageOffset(page, pdfView.zoom).toInt()
            pageX = pdfFile.getPageOffset(page, pdfView.zoom).toInt()
        }
        for (link in pdfFile.getPageLinks(page)) {
            val mapped = pdfFile.mapRectToDevice(
                pageIndex = page,
                startX = pageX,
                startY = pageY,
                sizeX = pageSize.width.toInt(),
                sizeY = pageSize.height.toInt(),
                rect = link.bounds
            )
            mapped.sort()
            if (mapped.contains(mappedX, mappedY)) {
                pdfView.callbacks.callLinkHandler(
                    LinkTapEvent(
                        originalX = x,
                        originalY = y,
                        documentX = mappedX,
                        documentY = mappedY,
                        mappedLinkRect = mapped,
                        link = link
                    )
                )
                return true
            }
        }
        return false
    }

    private fun startPageFling(
        downEvent: MotionEvent?,
        ev: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ) {
        if (!checkDoPageFling(velocityX, velocityY)) {
            return
        }
        val direction: Int = if (pdfView.isSwipeVertical) {
            if (velocityY > 0) -1 else 1
        } else {
            if (velocityX > 0) -1 else 1
        }
        // get the focused page during the down event to ensure only a single page is changed
        val delta = if (pdfView.isSwipeVertical) ev.y - downEvent!!.y else ev.x - downEvent!!.x
        val offsetX: Float = pdfView.currentXOffset - delta * pdfView.zoom
        val offsetY: Float = pdfView.currentYOffset - delta * pdfView.zoom
        val startingPage: Int = pdfView.findFocusPage(offsetX, offsetY)
        val targetPage =
            0.coerceAtLeast((pdfView.pageCount - 1).coerceAtMost(startingPage + direction))
        val edge: SnapEdge = pdfView.findSnapEdge(targetPage)
        val offset: Float = pdfView.snapOffsetForPage(targetPage, edge)
        animationManager.startPageFlingAnimation(-offset)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (!pdfView.isDoubleTapEnabled) {
            return false
        }
        if (pdfView.zoom < pdfView.midZoom) {
            pdfView.zoomWithAnimation(e.x, e.y, pdfView.midZoom)
        } else if (pdfView.zoom < pdfView.maxZoom) {
            pdfView.zoomWithAnimation(e.x, e.y, pdfView.maxZoom)
        } else {
            pdfView.resetZoomWithAnimation()
        }
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent) = false

    override fun onDown(e: MotionEvent): Boolean {
        animationManager.stopFling()
        return true
    }

    override fun onShowPress(e: MotionEvent) = Unit
    override fun onSingleTapUp(e: MotionEvent) = false

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float,
    ): Boolean {
        scrolling = true
        if (pdfView.isZooming || pdfView.isSwipeEnabled) {
            pdfView.moveRelativeTo(-distanceX, -distanceY)
        }
        if (!scaling || pdfView.doRenderDuringScale()) {
            pdfView.loadPageByOffset()
        }
        return true
    }

    private fun onScrollEnd() {
        pdfView.loadPages()
        hideHandle()
        if (!animationManager.isFlinging) {
            pdfView.performPageSnap()
        }
    }

    override fun onLongPress(e: MotionEvent) {
        pdfView.callbacks.callOnLongPress(e)
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        if (!pdfView.isSwipeEnabled) {
            return false
        }
        if (pdfView.isPageFlingEnabled) {
            if (pdfView.pageFillsScreen()) {
                onBoundedFling(velocityX, velocityY)
            } else {
                startPageFling(e1, e2, velocityX, velocityY)
            }
            return true
        }
        val xOffset = pdfView.currentXOffset.toInt()
        val yOffset = pdfView.currentYOffset.toInt()
        val minX: Float
        val minY: Float
        val pdfFile: PdfFile = pdfView.pdfFile!!
        if (pdfView.isSwipeVertical) {
            minX = -(pdfView.toCurrentScale(pdfFile.getMaxPageWidth()) - pdfView.width)
            minY = -(pdfFile.getDocLen(pdfView.zoom) - pdfView.height)
        } else {
            minX = -(pdfFile.getDocLen(pdfView.zoom) - pdfView.width)
            minY = -(pdfView.toCurrentScale(pdfFile.getMaxPageHeight()) - pdfView.height)
        }
        animationManager.startFlingAnimation(
            xOffset,
            yOffset,
            velocityX.toInt(),
            velocityY.toInt(),
            minX.toInt(),
            0,
            minY.toInt(),
            0
        )
        return true
    }

    private fun onBoundedFling(velocityX: Float, velocityY: Float) {
        val xOffset = pdfView.currentXOffset.toInt()
        val yOffset = pdfView.currentYOffset.toInt()
        val pdfFile: PdfFile = pdfView.pdfFile!!
        val pageStart = -pdfFile.getPageOffset(pdfView.currentPage, pdfView.zoom)
        val pageEnd = pageStart - pdfFile.getPageLength(pdfView.currentPage, pdfView.zoom)
        val minX: Float
        val minY: Float
        val maxX: Float
        val maxY: Float
        if (pdfView.isSwipeVertical) {
            minX = -(pdfView.toCurrentScale(pdfFile.getMaxPageWidth()) - pdfView.width)
            minY = pageEnd + pdfView.height
            maxX = 0f
            maxY = pageStart
        } else {
            minX = pageEnd + pdfView.width
            minY = -(pdfView.toCurrentScale(pdfFile.getMaxPageHeight()) - pdfView.height)
            maxX = pageStart
            maxY = 0f
        }
        animationManager.startFlingAnimation(
            xOffset,
            yOffset,
            velocityX.toInt(),
            velocityY.toInt(),
            minX.toInt(),
            maxX.toInt(),
            minY.toInt(),
            maxY.toInt()
        )
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        var dr = detector.getScaleFactor()
        val wantedZoom: Float = pdfView.zoom * dr
        val minZoom: Float = Math.min(MINIMUM_ZOOM, pdfView.minZoom)
        val maxZoom: Float = Math.min(MAXIMUM_ZOOM, pdfView.maxZoom)
        if (wantedZoom < minZoom) {
            dr = minZoom / pdfView.zoom
        } else if (wantedZoom > maxZoom) {
            dr = maxZoom / pdfView.zoom
        }
        pdfView.zoomCenteredRelativeTo(dr, PointF(detector.focusX, detector.focusY))
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        scaling = true
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        pdfView.loadPages()
        hideHandle()
        scaling = false
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (!enabled) {
            return false
        }
        var retVal = scaleGestureDetector.onTouchEvent(event)
        retVal = gestureDetector.onTouchEvent(event) || retVal
        if (event.action == MotionEvent.ACTION_UP) {
            if (scrolling) {
                scrolling = false
                onScrollEnd()
            }
        }
        return retVal
    }

    private fun hideHandle() {
        if (pdfView.scrollHandle?.shown == true) pdfView.scrollHandle?.hideDelayed()
    }

    private fun checkDoPageFling(velocityX: Float, velocityY: Float): Boolean {
        val absX = abs(velocityX.toDouble()).toFloat()
        val absY = abs(velocityY.toDouble()).toFloat()
        return if (pdfView.isSwipeVertical) absY > absX else absX > absY
    }
}