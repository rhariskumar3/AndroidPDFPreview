package com.harissk.pdfpreview.scroll

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.harissk.pdfpreview.PDFView
import com.harissk.pdfpreview.R
import com.harissk.pdfpreview.utils.toPx

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
    private val handler: Handler = Handler()
    private val hidePageScrollerRunnable = Runnable { hide() }

    init {
        visibility = INVISIBLE
        setTextColor(Color.BLACK)
        setTextSize(DEFAULT_TEXT_SIZE)
    }

    override fun setupLayout(pdfView: PDFView?) {
        val align: Int
        val width: Float
        val height: Float
        @DrawableRes val background: Int
        // determine handler position, default is right (when scrolling vertically) or bottom (when scrolling horizontally)
        when (pdfView?.isSwipeVertical) {
            true -> {
                width = HANDLE_LONG
                height = HANDLE_SHORT
                when {
                    inverted -> { // left
                        align = ALIGN_PARENT_LEFT
                        background = R.drawable.default_scroll_handle_left
                    }

                    else -> { // right
                        align = ALIGN_PARENT_RIGHT
                        background = R.drawable.default_scroll_handle_right
                    }
                }
            }

            else -> {
                width = HANDLE_SHORT
                height = HANDLE_LONG
                when {
                    inverted -> { // top
                        align = ALIGN_PARENT_TOP
                        background = R.drawable.default_scroll_handle_top
                    }

                    else -> { // bottom
                        align = ALIGN_PARENT_BOTTOM
                        background = R.drawable.default_scroll_handle_bottom
                    }
                }
            }
        }
        setBackground(ContextCompat.getDrawable(context, background))
        try {
            val lp = LayoutParams(context.toPx(width), context.toPx(height))
            lp.setMargins(0, 0, 0, 0)
            val tvlp = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            tvlp.addRule(CENTER_IN_PARENT, TRUE)
            if (textView.parent != null)
                (textView.parent as? ViewGroup)?.removeView(textView)
            addView(textView, tvlp)
            lp.addRule(align)

            pdfView?.let { safePdfView ->
                safePdfView.children.filterIsInstance<DefaultScrollHandle>()
                    .forEach { scrollHandle ->
                        safePdfView.removeView(scrollHandle)
                    }
            }
            pdfView?.addView(this, lp)
        } catch (e: Exception) {
            pdfView?.logWriter?.writeLog(e, javaClass.simpleName)
        }
        this.pdfView = pdfView
    }

    override fun destroyLayout() {
        pdfView?.removeView(this)
    }

    override fun setScroll(position: Float) {
        when {
            shown -> handler.removeCallbacks(hidePageScrollerRunnable)
            else -> show()
        }
        if (pdfView != null)
            setPosition((if (pdfView!!.isSwipeVertical) pdfView!!.height else pdfView!!.width) * position)
    }

    private fun setPosition(pos: Float) {
        var v = pos
        if (java.lang.Float.isInfinite(v) || java.lang.Float.isNaN(v)) return
        val pdfViewSize: Float = when (pdfView?.isSwipeVertical) {
            true -> pdfView!!.height.toFloat()
            else -> pdfView!!.width.toFloat()
        }
        v -= relativeHandlerMiddle
        when {
            v < 0 -> v = 0f
            v > pdfViewSize - context.toPx(HANDLE_SHORT) -> v =
                pdfViewSize - context.toPx(HANDLE_SHORT)
        }
        when (pdfView?.isSwipeVertical) {
            true -> y = v
            else -> x = v
        }
        calculateMiddle()
        invalidate()
    }

    private fun calculateMiddle() {
        val pos: Float
        val viewSize: Float
        val pdfViewSize: Float
        when (pdfView?.isSwipeVertical) {
            true -> {
                pos = y
                viewSize = height.toFloat()
                pdfViewSize = pdfView!!.height.toFloat()
            }

            else -> {
                pos = x
                viewSize = width.toFloat()
                pdfViewSize = pdfView!!.width.toFloat()
            }
        }
        relativeHandlerMiddle = (pos + relativeHandlerMiddle) / pdfViewSize * viewSize
    }

    override fun hideDelayed() {
        handler.postDelayed(hidePageScrollerRunnable, 1000)
    }

    override fun setPageNum(pageNum: Int) {
        val text = pageNum.toString()
        if (textView.getText() != text) textView.text = text
    }

    override val shown: Boolean
        get() = visibility == VISIBLE

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
    fun setTextSize(size: Int) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size.toFloat())
    }

    private fun isPDFViewReady(): Boolean =
        pdfView != null && pdfView!!.pageCount > 0 && !pdfView!!.documentFitsView()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isPDFViewReady()) return super.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                pdfView!!.stopFling()
                handler.removeCallbacks(hidePageScrollerRunnable)
                currentPos = when (pdfView?.isSwipeVertical) {
                    true -> event.rawY - y
                    else -> event.rawX - x
                }
                when (pdfView?.isSwipeVertical) {
                    true -> {
                        setPosition(event.rawY - currentPos + relativeHandlerMiddle)
                        pdfView?.setPositionOffset(relativeHandlerMiddle / height.toFloat(), false)
                    }

                    else -> {
                        setPosition(event.rawX - currentPos + relativeHandlerMiddle)
                        pdfView?.setPositionOffset(relativeHandlerMiddle / width.toFloat(), false)
                    }
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                when (pdfView?.isSwipeVertical) {
                    true -> {
                        setPosition(event.rawY - currentPos + relativeHandlerMiddle)
                        pdfView?.setPositionOffset(relativeHandlerMiddle / height.toFloat(), false)
                    }

                    else -> {
                        setPosition(event.rawX - currentPos + relativeHandlerMiddle)
                        pdfView?.setPositionOffset(relativeHandlerMiddle / width.toFloat(), false)
                    }
                }
                return true
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                hideDelayed()
                pdfView?.performPageSnap()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    companion object {
        private const val HANDLE_LONG = 65F
        private const val HANDLE_SHORT = 40F
        private const val DEFAULT_TEXT_SIZE = 16
    }
}