package com.harissk.pdfpreview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.PointF
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller

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
 * Handles animations for the PDFView.
 * This class uses ValueAnimator for smooth animations like zooming and scrolling,
 * and OverScroller for handling flings with momentum.
 */
internal class PdfAnimator(private val pdfView: PDFView) {

    private var activeAnimation: ValueAnimator? = null
    private val flingScroller = OverScroller(pdfView.context)
    private var flinging = false
    private var isPageAnimating = false

    fun animateHorizontal(startX: Float, targetX: Float) = animate(startX, targetX, false)

    fun animateVertical(startY: Float, targetY: Float) = animate(startY, targetY, true)

    private fun animate(startValue: Float, targetValue: Float, isAnimatingVertically: Boolean) {
        if (pdfView.isRecycled) return

        activeAnimation?.cancel() // Cancel any existing animation

        activeAnimation = ValueAnimator.ofFloat(startValue, targetValue).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                val animationValue = valueAnimator.animatedValue as Float
                pdfView.moveTo(
                    if (isAnimatingVertically) pdfView.currentXOffset else animationValue,
                    if (isAnimatingVertically) animationValue else pdfView.currentYOffset,
                    moveHandle = true
                )
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = handleAnimationEnd()
                override fun onAnimationCancel(animation: Animator) = handleAnimationEnd()
            })
            start()
        }
    }

    private fun handleAnimationEnd() {
        if (pdfView.isRecycled) return
        
        pdfView.updateScrollUIElements()
        pdfView.loadPages()
        isPageAnimating = false
        pdfView.scrollHandle?.hideDelayed()
    }

    fun zoomToPoint(
        zoomCenterX: Float,
        zoomCenterY: Float,
        startZoom: Float,
        targetZoom: Float,
    ) {
        if (pdfView.isRecycled) return

        cancelAllAnimations()
        activeAnimation = ValueAnimator.ofFloat(startZoom, targetZoom).apply {
            interpolator = DecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                pdfView.zoomCenteredTo(
                    valueAnimator.animatedValue as Float,
                    PointF(zoomCenterX, zoomCenterY)
                )
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    pdfView.loadPages()
                    pdfView.performPageSnap()
                    pdfView.scrollHandle?.hideDelayed()
                }

                override fun onAnimationCancel(animation: Animator) {
                    pdfView.loadPages()
                    pdfView.scrollHandle?.hideDelayed()
                }
            })
            duration = 400
            start()
        }
    }

    fun animateFling(
        flingStartX: Int,
        flingStartY: Int,
        flingVelocityX: Int,
        flingVelocityY: Int,
        flingMinX: Int,
        flingMaxX: Int,
        flingMinY: Int,
        flingMaxY: Int,
    ) {
        if (pdfView.isRecycled) {
            flinging = false
            return
        }
        cancelAllAnimations()
        flinging = true
        flingScroller.fling(
            /* startX = */ flingStartX,
            /* startY = */ flingStartY,
            /* velocityX = */ flingVelocityX,
            /* velocityY = */ flingVelocityY,
            /* minX = */ flingMinX,
            /* maxX = */ flingMaxX,
            /* minY = */ flingMinY,
            /* maxY = */ flingMaxY
        )
    }

    fun flingToPage(pageTargetOffset: Float) {
        if (pdfView.isRecycled) return
        if (pdfView.isSwipeVertical) animateVertical(pdfView.currentYOffset, pageTargetOffset)
        else animateHorizontal(pdfView.currentXOffset, pageTargetOffset)
        isPageAnimating = true
    }

    fun performFling() {
        if (pdfView.isRecycled || !flinging) return
        when {
            flingScroller.computeScrollOffset() -> {
                pdfView.moveTo(flingScroller.currX.toFloat(), flingScroller.currY.toFloat(), moveHandle = true)
                pdfView.invalidate()
            }

            else -> {
                flinging = false
                if (!pdfView.isRecycled) {
                    pdfView.updateScrollUIElements()
                    pdfView.loadPages()
                    pdfView.scrollHandle?.hideDelayed()
                    pdfView.performPageSnap()
                }
            }
        }
    }

    fun cancelAllAnimations() {
        activeAnimation?.cancel()
        activeAnimation = null
        cancelFling()
    }

    fun cancelFling() {
        flinging = false
        flingScroller.forceFinished(true)
    }

    val isFlinging: Boolean
        get() = flinging || isPageAnimating
}