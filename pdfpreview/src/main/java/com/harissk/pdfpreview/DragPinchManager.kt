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
import kotlin.math.abs
import kotlin.math.roundToInt

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
 * Manages interactions with the PDFView, handling gestures like scrolling, zooming, and page
 * flipping.
 */
internal class DragPinchManager(
    private val pdfView: PDFView,
    private val animationManager: AnimationManager,
) :
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener,
    OnScaleGestureListener,
    OnTouchListener {
    private val gestureDetector by lazy { GestureDetector(pdfView.context, this); }
    private val scaleGestureDetector by lazy { ScaleGestureDetector(pdfView.context, this); }
    private var scrolling = false
    private var scaling = false
    private var enabled = false

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
        if (!pdfView.callOnTap(e) && !checkLinkTapped(e.x, e.y)) {
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
        val pdfFile: PdfFile = pdfView.pdfFile
        val mappedX: Float = -pdfView.currentXOffset + x
        val mappedY: Float = -pdfView.currentYOffset + y
        val page = pdfFile.getPageAtOffset(
            if (pdfView.isSwipeVertical) mappedY else mappedX,
            pdfView.zoom
        )
        val pageSize: SizeF = pdfFile.getScaledPageSize(page, pdfView.zoom)
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
        downEvent: MotionEvent?,
        ev: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ) {
        if (!checkDoPageFling(velocityX, velocityY)) return
        val direction = when (pdfView.isSwipeVertical) {
            true -> if (velocityY > 0) -1 else 1
            false -> if (velocityX > 0) -1 else 1
        }
        // get the focused page during the down event to ensure only a single page is changed
        val delta = when {
            pdfView.isSwipeVertical -> ev.y - (downEvent?.y ?: 0F)
            else -> ev.x - (downEvent?.x ?: 0F)
        }
        val offsetX: Float = pdfView.currentXOffset - delta * pdfView.zoom
        val offsetY: Float = pdfView.currentYOffset - delta * pdfView.zoom
        val startingPage: Int = pdfView.findFocusPage(offsetX, offsetY)
        val targetPage = (startingPage + direction).coerceIn(0, pdfView.pageCount - 1)
        animationManager.startPageFlingAnimation(
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
        animationManager.stopFling()
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
        if (!scaling || pdfView.doRenderDuringScale) pdfView.loadPageByOffset()
        return true
    }

    private fun onScrollEnd() {
        pdfView.loadPages()
        hideHandle()
        if (!animationManager.isFlinging) pdfView.performPageSnap()
    }

    override fun onLongPress(e: MotionEvent) = pdfView.callOnLongPress(e)

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        if (!pdfView.isSwipeEnabled) return false
        val pdfFile = pdfView.pdfFile
        when {
            pdfView.isPageFlingEnabled -> {
                when {
                    pdfView.pageFillsScreen() -> onBoundedFling(velocityX, velocityY)
                    else -> startPageFling(e1, e2, velocityX, velocityY)
                }
                return true
            }

            else -> {
                val xOffset = pdfView.currentXOffset.toInt()
                val yOffset = pdfView.currentYOffset.toInt()
                val minX = when (pdfView.isSwipeVertical) {
                    true -> -(pdfView.toCurrentScale(pdfFile.maxPageWidth) - pdfView.width)
                    false -> -(pdfFile.getDocLen(pdfView.zoom) - pdfView.width)
                }.roundToInt()
                val minY = when (pdfView.isSwipeVertical) {
                    true -> -(pdfFile.getDocLen(pdfView.zoom) - pdfView.height)
                    false -> -(pdfView.toCurrentScale(pdfFile.maxPageHeight) - pdfView.height)
                }.roundToInt()
                animationManager.startFlingAnimation(
                    startX = xOffset,
                    startY = yOffset,
                    velocityX = velocityX.toInt(),
                    velocityY = velocityY.toInt(),
                    minX = minX,
                    maxX = 0,
                    minY = minY,
                    maxY = 0
                )
                return true
            }
        }
    }

    private fun onBoundedFling(velocityX: Float, velocityY: Float) {
        val pdfFile = pdfView.pdfFile
        val pageStart = -pdfFile.getPageOffset(pdfView.currentPage, pdfView.zoom)
        val pageEnd = pageStart - pdfFile.getPageLength(pdfView.currentPage, pdfView.zoom)
        val minX: Float
        val minY: Float
        val maxX: Float
        val maxY: Float
        when {
            pdfView.isSwipeVertical -> {
                minX = -(pdfView.toCurrentScale(pdfFile.maxPageWidth) - pdfView.width)
                minY = pageEnd + pdfView.height
                maxX = 0f
                maxY = pageStart
            }

            else -> {
                minX = pageEnd + pdfView.width
                minY = -(pdfView.toCurrentScale(pdfFile.maxPageHeight) - pdfView.height)
                maxX = pageStart
                maxY = 0f
            }
        }
        animationManager.startFlingAnimation(
            startX = pdfView.currentXOffset.toInt(),
            startY = pdfView.currentYOffset.toInt(),
            velocityX = velocityX.toInt(),
            velocityY = velocityY.toInt(),
            minX = minX.toInt(),
            maxX = maxX.toInt(),
            minY = minY.toInt(),
            maxY = maxY.toInt()
        )
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        var dr = detector.getScaleFactor()
        val wantedZoom: Float = pdfView.zoom * dr
        val minZoom: Float = pdfView.renderOptions.pinchMinimumZoom.coerceAtMost(pdfView.minZoom)
        val maxZoom: Float = pdfView.renderOptions.pinchMaximumZoom.coerceAtMost(pdfView.maxZoom)
        when {
            wantedZoom < minZoom -> dr = minZoom / pdfView.zoom

            wantedZoom > maxZoom -> dr = maxZoom / pdfView.zoom
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
        if (!enabled) return false
        val retVal = gestureDetector.onTouchEvent(event) || scaleGestureDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP && scrolling) {
            scrolling = false
            onScrollEnd()
        }
        return retVal
    }

    private fun hideHandle() {
        if (pdfView.scrollHandle?.shown == true) pdfView.scrollHandle?.hideDelayed()
    }

    private fun checkDoPageFling(velocityX: Float, velocityY: Float): Boolean = when {
        pdfView.isSwipeVertical -> abs(velocityY) > abs(velocityX)
        else -> abs(velocityX) > abs(velocityY)
    }
}