package com.harissk.pdfpreview

import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.View
import android.view.View.OnTouchListener
import com.harissk.pdfpreview.model.LinkTapEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
 * This Manager takes care of moving the PDFView,
 * set its zoom track user actions.
 */
internal class DragPinchManager(
    private val pdfView: PDFView,
    private val pdfAnimator: PdfAnimator,
) : GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener,
    OnScaleGestureListener,
    OnTouchListener {

    private val gestureDetector by lazy { GestureDetector(pdfView.context, this) }
    private val scaleGestureDetector by lazy { ScaleGestureDetector(pdfView.context, this) }

    private var lastX = 0f
    private var lastY = 0f

    private var lastZoomLevel = 1f
    private var lastZoomReloadTime = 0L

    private var scrolling = false
    private var scaling = false
    private var enabled = false

    /** Check if user is currently scrolling */
    val isScrolling: Boolean get() = scrolling

    init {
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
        pdfView.callOnTap(e)
        checkLinkTapped(e.x, e.y)

        if (!pdfView.documentFitsView())
            when {
                pdfView.scrollHandle?.shown == true -> pdfView.scrollHandle?.hide()
                else -> pdfView.scrollHandle?.show()
            }
        pdfView.performClick()
        return true
    }

    private fun checkLinkTapped(x: Float, y: Float): Boolean {
        val pdfFile = pdfView.pdfFile
        val mappedX = -pdfView.currentXOffset + x
        val mappedY = -pdfView.currentYOffset + y
        val page = pdfFile.getPageAtOffset(
            if (pdfView.isSwipeVertical) mappedY else mappedX, pdfView.zoom
        )
        val pageSize = pdfFile.getScaledPageSize(page, pdfView.zoom)
        val pageX: Int
        val pageY: Int
        when {
            pdfView.isSwipeVertical -> {
                pageX = pdfFile.getSecondaryPageOffset(page, pdfView.zoom).toInt()
                pageY = pdfFile.getPageOffset(page, pdfView.zoom).toInt()
            }

            else -> {
                pageY = pdfFile.getSecondaryPageOffset(page, pdfView.zoom).toInt()
                pageX = pdfFile.getPageOffset(page, pdfView.zoom).toInt()
            }
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
                pdfView.callLinkHandler(
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
        downEvent: MotionEvent,
        ev: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ) {
        if (!checkDoPageFling(velocityX, velocityY)) return
        val direction = when {
            pdfView.isSwipeVertical -> if (velocityY > 0) -1 else 1
            else -> if (velocityX > 0) -1 else 1
        }
        // get the focused page during the down event to ensure only a single page is changed
        val delta = if (pdfView.isSwipeVertical) ev.y - downEvent.y else ev.x - downEvent.x
        val offsetX = pdfView.currentXOffset - delta * pdfView.zoom
        val offsetY = pdfView.currentYOffset - delta * pdfView.zoom
        val startingPage: Int = pdfView.findFocusPage(offsetX, offsetY)
        val targetPage = max(0, min(pdfView.pageCount - 1, startingPage + direction))
        pdfAnimator.flingToPage(
            -pdfView.snapOffsetForPage(
                targetPage,
                pdfView.findSnapEdge(targetPage)
            )
        )
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (!pdfView.isDoubleTapEnabled) return false
        when {
            pdfView.zoom < pdfView.midZoom -> pdfView.zoomWithAnimation(e.x, e.y, pdfView.midZoom)
            pdfView.zoom < pdfView.maxZoom -> pdfView.zoomWithAnimation(e.x, e.y, pdfView.maxZoom)
            else -> pdfView.resetZoomWithAnimation()
        }
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent) = false

    override fun onDown(e: MotionEvent): Boolean {
        pdfAnimator.cancelFling()
        return true
    }

    override fun onShowPress(e: MotionEvent) {
        // NO-OP
    }

    override fun onSingleTapUp(e: MotionEvent) = false

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float,
    ): Boolean {
        scrolling = true
        if (pdfView.isZooming || pdfView.isSwipeEnabled)
            pdfView.moveRelativeTo(-distanceX, -distanceY)
        return true
    }

    private fun onScrollEnd() {
        pdfView.updateScrollUIElements()
        pdfView.loadPageByOffset()  // Check if page changed and fire onPageChanged callback
        hideHandle()
        if (!pdfAnimator.isFlinging) pdfView.performPageSnap()
    }

    override fun onLongPress(e: MotionEvent) = pdfView.callOnLongPress(e)

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        if (!pdfView.isSwipeEnabled) return false
        when {
            pdfView.isPageFlingEnabled -> when {
                pdfView.pageFillsScreen() -> onBoundedFling(velocityX, velocityY)
                else -> e1?.let { startPageFling(it, e2, velocityX, velocityY) }
            }

            else -> pdfAnimator.animateFling(
                flingStartX = pdfView.currentXOffset.toInt(),
                flingStartY = pdfView.currentYOffset.toInt(),
                flingVelocityX = velocityX.toInt(),
                flingVelocityY = velocityY.toInt(),
                flingMinX = when {
                    pdfView.isSwipeVertical -> -(pdfView.toCurrentScale(pdfView.pdfFile.maxPageWidth) - pdfView.width)
                    else -> -(pdfView.pdfFile.getDocLen(pdfView.zoom) - pdfView.width)
                }.toInt(),
                flingMaxX = 0,
                flingMinY = when {
                    pdfView.isSwipeVertical -> -(pdfView.pdfFile.getDocLen(pdfView.zoom) - pdfView.height)
                    else -> -(pdfView.toCurrentScale(pdfView.pdfFile.maxPageHeight) - pdfView.height)
                }.toInt(),
                flingMaxY = 0
            )
        }
        return true
    }

    private fun onBoundedFling(velocityX: Float, velocityY: Float) {
        val xOffset = pdfView.currentXOffset
        val yOffset = pdfView.currentYOffset

        val mappedX = -xOffset + lastX
        val mappedY = -yOffset + lastY

        val page = pdfView.pdfFile.getPageAtOffset(
            offset = if (pdfView.isSwipeVertical) mappedY else mappedX,
            zoom = pdfView.zoom
        )

        val pageStart = -pdfView.pdfFile.getPageOffset(page, pdfView.zoom)
        val pageEnd: Float = pageStart - pdfView.pdfFile.getPageLength(page, pdfView.zoom)
        val minX: Float
        val minY: Float
        val maxX: Float
        val maxY: Float
        when {
            pdfView.isSwipeVertical -> {
                minX = -(pdfView.toCurrentScale(pdfView.pdfFile.maxPageWidth) - pdfView.width)
                minY = pageEnd + pdfView.height
                maxX = 0f
                maxY = pageStart
            }

            else -> {
                minX = pageEnd + pdfView.width
                minY = -(pdfView.toCurrentScale(pdfView.pdfFile.maxPageHeight) - pdfView.height)
                maxX = pageStart
                maxY = 0f
            }
        }
        pdfAnimator.animateFling(
            flingStartX = xOffset.toInt(),
            flingStartY = yOffset.toInt(),
            flingVelocityX = velocityX.toInt(),
            flingVelocityY = velocityY.toInt(),
            flingMinX = minX.toInt(),
            flingMaxX = maxX.toInt(),
            flingMinY = minY.toInt(),
            flingMaxY = maxY.toInt()
        )
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        val wantedZoom = pdfView.zoom * detector.scaleFactor
        val minZoom = pdfView.pdfViewerConfiguration.minZoom.coerceAtMost(pdfView.minZoom)
        val maxZoom = pdfView.pdfViewerConfiguration.maxZoom.coerceAtMost(pdfView.maxZoom)

        pdfView.zoomCenteredRelativeTo(
            dzoom = wantedZoom.coerceIn(minZoom, maxZoom) / pdfView.zoom,
            pivot = PointF(detector.focusX, detector.focusY)
        )

        // Reload pages periodically during scaling to reduce blur time
        val currentTime = System.currentTimeMillis()
        val zoomChange = abs(pdfView.zoom - lastZoomLevel) / lastZoomLevel
        if (zoomChange > 0.2f && (currentTime - lastZoomReloadTime) > 200) { // Every 200ms and 20% zoom change
            lastZoomLevel = pdfView.zoom
            lastZoomReloadTime = currentTime
            pdfView.loadPages()
        }

        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        scaling = true
        lastZoomLevel = pdfView.zoom
        lastZoomReloadTime = System.currentTimeMillis()
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        scaling = false
        val finalZoom = pdfView.zoom
        pdfView.loadPages()
        hideHandle()
        pdfView.zoomTo(finalZoom)

        pdfView.notifyZoomChanged(newZoom = finalZoom, oldZoom = lastZoomLevel)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (!enabled) return false
        val isGestureEvent = gestureDetector.onTouchEvent(event)
        val isScaleGestureEvent = scaleGestureDetector.onTouchEvent(event)

        lastX = event.x
        lastY = event.y

        if (event.action == MotionEvent.ACTION_UP && scrolling) {
            scrolling = false
            onScrollEnd()
        }
        return isGestureEvent || isScaleGestureEvent
    }

    private fun hideHandle() {
        if (pdfView.scrollHandle?.shown == true) pdfView.scrollHandle?.hideDelayed()
    }

    private fun checkDoPageFling(velocityX: Float, velocityY: Float): Boolean = when {
        pdfView.isSwipeVertical -> abs(velocityY) > abs(velocityX)
        else -> abs(velocityX) > abs(velocityY)
    }
}