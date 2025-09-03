package com.harissk.pdfpreview.scroll

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.view.children
import androidx.core.view.isVisible
import com.harissk.pdfpreview.PDFView
import com.harissk.pdfpreview.utils.toPx

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
 * Default implementation of the {@link ScrollHandle} interface that provides a visual indicator for
 * scrolling within the PDF view.
 *
 * @param context The context to use for creating the scroll handle.
 * @param inverted Whether the scroll handle should be positioned inverted.
 */
@SuppressLint("ViewConstructor")
class DefaultScrollHandle(private val context: Context, private val inverted: Boolean = false) :
    RelativeLayout(context), ScrollHandle {

    private var relativeHandlerMiddle = 0f
    private val textView = TextView(context)
    private var pdfView: PDFView? = null
    private var currentPos = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val hidePageScrollerRunnable = Runnable { hide() }

    // Estimate for handle thickness if not measured yet (text height + vertical paddings)
    private val estimatedMinThicknessPx: Float by lazy {
        (context.toPx(DEFAULT_TEXT_SIZE) + context.toPx(DEFAULT_TEXT_PADDING_VERTICAL) * 2).toFloat()
    }

    init {
        visibility = INVISIBLE
        setTextSize(DEFAULT_TEXT_SIZE)
    }

    override fun setupLayout(pdfView: PDFView?) {
        if (pdfView == null || pdfView.isRecycled) return

        val nightModeFlags =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

        val handleColor: Int
        val textColor: Int

        when {
            isNightMode -> {
                handleColor = "#333333".toColorInt() // Darker Gray for handle background
                textColor = "#E0E0E0".toColorInt() // Light Gray for text
            }

            else -> {
                handleColor = "#E0E0E0".toColorInt() // Lighter Gray for handle background
                textColor = "#212121".toColorInt() // Dark Gray for text
            }
        }

        setTextColor(textColor)
        textView.maxLines = 1 // Ensure text is on a single line

        // Padding for this RelativeLayout (the handle itself)
        val horizontalPadding = context.toPx(DEFAULT_TEXT_PADDING_HORIZONTAL)
        val verticalPadding = context.toPx(DEFAULT_TEXT_PADDING_VERTICAL)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        // Determine alignment based on swipe direction
        val align: Int = when {
            pdfView.isSwipeVertical -> when {
                inverted -> ALIGN_PARENT_LEFT
                else -> ALIGN_PARENT_RIGHT
            }

            else -> when {
                inverted -> ALIGN_PARENT_TOP
                else -> ALIGN_PARENT_BOTTOM
            }
        }

        // LayoutParams for adding this handle to PDFView
        val layoutParamsForPdfView =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        layoutParamsForPdfView.addRule(align)

        // Create a modern, pill-shaped drawable programmatically
        val handleDrawable = GradientDrawable().apply {
            setColor(handleColor)
            // Use a large corner radius to ensure a pill shape, capped at min(width, height) / 2
            setCornerRadius(context.toPx(100f).toFloat()) // A large value like 100dp
        }
        this.background = handleDrawable
        this.elevation = context.toPx(DEFAULT_ELEVATION_DP).toFloat()

        val tvlp = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        )
        tvlp.addRule(CENTER_IN_PARENT, TRUE)
        if (textView.parent != null) (textView.parent as? ViewGroup)?.removeView(textView)
        addView(textView, tvlp)

        pdfView.children.filterIsInstance<DefaultScrollHandle>()
            .forEach { scrollHandle ->
                pdfView.removeView(scrollHandle)
            }
        pdfView.addView(this, layoutParamsForPdfView)
        this.pdfView = pdfView
    }

    override fun destroyLayout() {
        pdfView?.removeView(this)
    }

    override fun setScroll(position: Float) {
        pdfView?.takeUnless { it.isRecycled } ?: return

        if (!shown) show()
        handler.removeCallbacks(hidePageScrollerRunnable)
        val targetPos = when (pdfView?.isSwipeVertical) {
            true -> (pdfView?.height ?: 0) * position
            else -> (pdfView?.width ?: 0) * position
        }
        setPosition(targetPos)
    }

    private fun setPosition(pos: Float) {
        if (pos.isNaN() || pos.isInfinite()) return
        val currentPdfView = pdfView ?: return

        val pdfViewSize: Float = when (currentPdfView.isSwipeVertical) {
            true -> currentPdfView.height.toFloat()
            else -> currentPdfView.width.toFloat()
        }

        val currentHandleHeight = this.height.toFloat().takeIf { it > 0 } ?: estimatedMinThicknessPx
        val currentHandleWidth = this.width.toFloat().takeIf { it > 0 } ?: estimatedMinThicknessPx

        val handleDimension = when {
            currentPdfView.isSwipeVertical -> currentHandleHeight
            else -> currentHandleWidth
        }

        var v = pos - relativeHandlerMiddle
        // Ensure handleDimension is positive to prevent NaN in coerceIn if pdfViewSize is small
        v = v.coerceIn(0f, pdfViewSize - (handleDimension.takeIf { it > 0 } ?: 0f))

        if (currentPdfView.isSwipeVertical) y = v else x = v

        calculateMiddle()
        invalidate()
    }

    private fun calculateMiddle() {
        pdfView?.let { view ->
            val h = height.toFloat().takeIf { it > 0 } ?: estimatedMinThicknessPx
            val w = width.toFloat().takeIf { it > 0 } ?: estimatedMinThicknessPx

            val (pos, viewSize, pdfViewSize) = when {
                view.isSwipeVertical -> Triple(y, h, view.height.toFloat())
                else -> Triple(x, w, view.width.toFloat())
            }
            if (pdfViewSize > 0 && viewSize > 0)
                relativeHandlerMiddle = (pos + relativeHandlerMiddle) / pdfViewSize * viewSize
        }
    }

    override fun hideDelayed() {
        handler.postDelayed(hidePageScrollerRunnable, 1000)
    }

    override fun setPageNum(pageNum: Int) {
        val totalPages = pdfView?.pageCount ?: 0
        val text = "$pageNum/$totalPages"
        if (textView.text != text) textView.text = text
    }

    override val shown: Boolean
        get() = isVisible

    override fun show() {
        visibility = VISIBLE
    }

    override fun hide() {
        visibility = INVISIBLE
    }

    fun setTextColor(color: Int) = textView.setTextColor(color)

    /**
     * @param size text size in dp
     */
    fun setTextSize(size: Float) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
    }

    private fun isPDFViewReady(): Boolean =
        pdfView != null && pdfView!!.pageCount > 0 && !pdfView!!.isRecycled

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isPDFViewReady()) return super.onTouchEvent(event)
        val currentPdfView = pdfView!!

        val currentHandleHeight = height.toFloat().takeIf { it > 0 } ?: estimatedMinThicknessPx
        val currentHandleWidth = width.toFloat().takeIf { it > 0 } ?: estimatedMinThicknessPx

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                currentPdfView.stopFling()
                currentPdfView.setScrollHandleDragging(true)
                handler.removeCallbacks(hidePageScrollerRunnable)
                currentPos = when (currentPdfView.isSwipeVertical) {
                    true -> event.rawY - y
                    else -> event.rawX - x
                }
                val eventPos = if (currentPdfView.isSwipeVertical) event.rawY else event.rawX
                setPosition(eventPos - currentPos + relativeHandlerMiddle)
                currentPdfView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val eventPos = if (currentPdfView.isSwipeVertical) event.rawY else event.rawX
                setPosition(eventPos - currentPos + relativeHandlerMiddle)

                val handleCenter = when {
                    currentPdfView.isSwipeVertical -> y + currentHandleHeight / 2f
                    else -> x + currentHandleWidth / 2f
                }
                val viewSize = when {
                    currentPdfView.isSwipeVertical -> currentPdfView.height.toFloat()
                    else -> currentPdfView.width.toFloat()
                }
                val actualHandleSize = when {
                    currentPdfView.isSwipeVertical -> currentHandleHeight
                    else -> currentHandleWidth
                }

                if (viewSize > actualHandleSize && actualHandleSize > 0) {
                    val offset =
                        (handleCenter - actualHandleSize / 2f) / (viewSize - actualHandleSize)
                    currentPdfView.setPositionOffset(offset.coerceIn(0f, 1f), false)
                    
                    val actualCurrentPage = currentPdfView.getPageAtPositionOffset(offset.coerceIn(0f, 1f))
                    setPageNum(actualCurrentPage + 1)
                }
                return true
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                currentPdfView.setScrollHandleDragging(false)
                currentPdfView.updateScrollUIElements()
                currentPdfView.loadPageByOffset()
                hideDelayed()
                currentPdfView.performPageSnap()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    companion object {
        private const val DEFAULT_TEXT_PADDING_HORIZONTAL = 12f // dp
        private const val DEFAULT_TEXT_PADDING_VERTICAL = 6f  // dp
        private const val DEFAULT_TEXT_SIZE = 12f // sp, for page number text
        private const val DEFAULT_ELEVATION_DP = 2f // dp, for shadow
    }
}