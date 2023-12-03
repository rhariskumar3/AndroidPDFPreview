package com.harissk.pdfpreview

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.graphics.PointF
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller


/**
 * Created by Harishkumar on 25/11/23.
 */
/**
 * Handles animations for the PDFView.
 *
 * Utilizes the ValueAnimator introduced in API 11 to initiate animations and
 * update the PDFView's position based on animation updates.
 */
internal class AnimationManager(private val pdfView: PDFView) {

    private var animation: ValueAnimator? = null
    private val scroller = OverScroller(pdfView.context)
    private var flinging = false
    private var pageFlinging = false

    /**
     * Initiates an animation transitioning the PDFView's horizontal position from `xFrom` to `xTo`.
     */
    fun startXAnimation(xFrom: Float, xTo: Float) {
        stopAll()
        animation = ValueAnimator.ofFloat(xFrom, xTo)
        val xAnimation = XAnimation()
        animation?.interpolator = DecelerateInterpolator()
        animation?.addUpdateListener(xAnimation)
        animation?.addListener(xAnimation)
        animation?.setDuration(400)
        animation?.start()
    }

    /**
     * Initiates an animation transitioning the PDFView's vertical position from `yFrom` to `yTo`.
     */
    fun startYAnimation(yFrom: Float, yTo: Float) {
        stopAll()
        animation = ValueAnimator.ofFloat(yFrom, yTo)
        val yAnimation = YAnimation()
        animation?.interpolator = DecelerateInterpolator()
        animation?.addUpdateListener(yAnimation)
        animation?.addListener(yAnimation)
        animation?.setDuration(400)
        animation?.start()
    }

    /**
     * Initiates an animation that zooms the PDFView around the specified center point (`centerX`, `centerY`) from `zoomFrom` to `zoomTo`.
     */
    fun startZoomAnimation(centerX: Float, centerY: Float, zoomFrom: Float, zoomTo: Float) {
        stopAll()
        animation = ValueAnimator.ofFloat(zoomFrom, zoomTo)
        animation?.interpolator = DecelerateInterpolator()
        val zoomAnim = ZoomAnimation(centerX, centerY)
        animation?.addUpdateListener(zoomAnim)
        animation?.addListener(zoomAnim)
        animation?.setDuration(400)
        animation?.start()
    }

    /**
     * Initiates a fling animation that transitions the PDFView's position based on the specified initial velocity and scroll constraints.
     */
    fun startFlingAnimation(
        startX: Int,
        startY: Int,
        velocityX: Int,
        velocityY: Int,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
    ) {
        stopAll()
        flinging = true
        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
    }

    /**
     * Initiates a page-flipping animation that transitions the PDFView's position to the specified target offset.
     */
    fun startPageFlingAnimation(targetOffset: Float) {
        when {
            pdfView.isSwipeVertical -> startYAnimation(pdfView.currentYOffset, targetOffset)
            else -> startXAnimation(pdfView.currentXOffset, targetOffset)
        }
        pageFlinging = true
    }

    fun computeFling() {
        when {
            scroller.computeScrollOffset() -> {
                pdfView.moveTo(scroller.currX.toFloat(), scroller.currY.toFloat())
                pdfView.loadPageByOffset()
            }
            // fling finished
            flinging -> {
                flinging = false
                pdfView.loadPages()
                pdfView.scrollHandle?.hideDelayed()
                pdfView.performPageSnap()
            }
        }
    }

    fun stopAll() {
        animation?.cancel()
        animation = null
        stopFling()
    }

    fun stopFling() {
        flinging = false
        scroller.forceFinished(true)
    }

    val isFlinging: Boolean
        get() = flinging || pageFlinging

    internal inner class XAnimation : AnimatorListenerAdapter(), AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val offset = animation.getAnimatedValue() as Float
            pdfView.moveTo(offset, pdfView.currentYOffset)
            pdfView.loadPageByOffset()
        }

        override fun onAnimationCancel(animation: Animator) = handleAnimationEnd()

        override fun onAnimationEnd(animation: Animator) = handleAnimationEnd()

        private fun handleAnimationEnd() {
            pdfView.loadPages()
            pageFlinging = false
            pdfView.scrollHandle?.hideDelayed()
        }
    }

    internal inner class YAnimation : AnimatorListenerAdapter(), AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val offset = animation.getAnimatedValue() as Float
            pdfView.moveTo(pdfView.currentXOffset, offset)
            pdfView.loadPageByOffset()
        }

        override fun onAnimationCancel(animation: Animator) = handleAnimationEnd()

        override fun onAnimationEnd(animation: Animator) = handleAnimationEnd()

        private fun handleAnimationEnd() {
            pdfView.loadPages()
            pageFlinging = false
            pdfView.scrollHandle?.hideDelayed()
        }
    }

    internal inner class ZoomAnimation(private val centerX: Float, private val centerY: Float) :
        AnimatorUpdateListener, AnimatorListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val zoom = animation.getAnimatedValue() as Float
            pdfView.zoomCenteredTo(zoom, PointF(centerX, centerY))
        }

        override fun onAnimationCancel(animation: Animator) {
            pdfView.loadPages()
            pdfView.scrollHandle?.hideDelayed()
        }

        override fun onAnimationEnd(animation: Animator) {
            pdfView.loadPages()
            pdfView.performPageSnap()
            pdfView.scrollHandle?.hideDelayed()
        }

        override fun onAnimationRepeat(animation: Animator) = Unit
        override fun onAnimationStart(animation: Animator) = Unit
    }
}