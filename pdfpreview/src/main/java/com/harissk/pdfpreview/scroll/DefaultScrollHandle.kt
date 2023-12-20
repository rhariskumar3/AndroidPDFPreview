package com.harissk.pdfpreview.scroll

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.harissk.pdfpreview.PDFView
import com.harissk.pdfpreview.R
import com.harissk.pdfpreview.utils.toPx


/**
 * Created by Harishkumar on 25/11/23.
 */

class DefaultScrollHandle(private val context: Context, private val inverted: Boolean = false) :
    RelativeLayout(context), ScrollHandle {

    private var relativeHandlerMiddle = 0f
    private var textView = TextView(context)
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
        val background: Drawable?
        // determine handler position, default is right (when scrolling vertically) or bottom (when scrolling horizontally)
        if (pdfView?.isSwipeVertical == true) {
            width = HANDLE_LONG
            height = HANDLE_SHORT
            if (inverted) { // left
                align = ALIGN_PARENT_LEFT
                background =
                    ContextCompat.getDrawable(context, R.drawable.default_scroll_handle_left)
            } else { // right
                align = ALIGN_PARENT_RIGHT
                background =
                    ContextCompat.getDrawable(context, R.drawable.default_scroll_handle_right)
            }
        } else {
            width = HANDLE_SHORT
            height = HANDLE_LONG
            if (inverted) { // top
                align = ALIGN_PARENT_TOP
                background =
                    ContextCompat.getDrawable(context, R.drawable.default_scroll_handle_top)
            } else { // bottom
                align = ALIGN_PARENT_BOTTOM
                background =
                    ContextCompat.getDrawable(context, R.drawable.default_scroll_handle_bottom)
            }
        }
        setBackground(background)
        val lp = LayoutParams(context.toPx(width), context.toPx(height))
        lp.setMargins(0, 0, 0, 0)
        val tvlp =
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        tvlp.addRule(CENTER_IN_PARENT, TRUE)
        if (textView.parent != null)
            (textView.parent as ViewGroup).removeView(textView)
        addView(textView, tvlp)
        lp.addRule(align)
        pdfView?.addView(this, lp)
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
        if (pdfView != null) {
            setPosition((if (pdfView!!.isSwipeVertical) pdfView!!.height else pdfView!!.width) * position)
        }
    }

    private fun setPosition(pos: Float) {
        var pos = pos
        if (java.lang.Float.isInfinite(pos) || java.lang.Float.isNaN(pos)) {
            return
        }
        val pdfViewSize: Float = if (pdfView!!.isSwipeVertical) {
            pdfView!!.height.toFloat()
        } else {
            pdfView!!.width.toFloat()
        }
        pos -= relativeHandlerMiddle
        if (pos < 0) {
            pos = 0f
        } else if (pos > pdfViewSize - context.toPx(HANDLE_SHORT)) {
            pos = pdfViewSize - context.toPx(HANDLE_SHORT)
        }
        if (pdfView!!.isSwipeVertical) {
            y = pos
        } else {
            x = pos
        }
        calculateMiddle()
        invalidate()
    }

    private fun calculateMiddle() {
        val pos: Float
        val viewSize: Float
        val pdfViewSize: Float
        if (pdfView!!.isSwipeVertical) {
            pos = y
            viewSize = height.toFloat()
            pdfViewSize = pdfView!!.height.toFloat()
        } else {
            pos = x
            viewSize = width.toFloat()
            pdfViewSize = pdfView!!.width.toFloat()
        }
        relativeHandlerMiddle = (pos + relativeHandlerMiddle) / pdfViewSize * viewSize
    }

    override fun hideDelayed() {
        handler.postDelayed(hidePageScrollerRunnable, 1000)
    }

    override fun setPageNum(pageNum: Int) {
        val text = pageNum.toString()
        if (textView.getText() != text) {
            textView.text = text
        }
    }

    override val shown: Boolean
        get() = visibility == VISIBLE

    override fun show() {
        visibility = VISIBLE
    }

    override fun hide() {
        visibility = INVISIBLE
    }

    fun setTextColor(color: Int) {
        textView.setTextColor(color)
    }

    /**
     * @param size text size in dp
     */
    fun setTextSize(size: Int) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size.toFloat())
    }

    private fun isPDFViewReady(): Boolean {
        return pdfView != null && pdfView!!.pageCount > 0 && !pdfView!!.documentFitsView()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isPDFViewReady()) {
            return super.onTouchEvent(event)
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                pdfView!!.stopFling()
                handler.removeCallbacks(hidePageScrollerRunnable)
                currentPos = if (pdfView!!.isSwipeVertical) {
                    event.rawY - y
                } else {
                    event.rawX - x
                }
                if (pdfView!!.isSwipeVertical) {
                    setPosition(event.rawY - currentPos + relativeHandlerMiddle)
                    pdfView!!.setPositionOffset(relativeHandlerMiddle / height.toFloat(), false)
                } else {
                    setPosition(event.rawX - currentPos + relativeHandlerMiddle)
                    pdfView!!.setPositionOffset(relativeHandlerMiddle / width.toFloat(), false)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (pdfView!!.isSwipeVertical) {
                    setPosition(event.rawY - currentPos + relativeHandlerMiddle)
                    pdfView!!.setPositionOffset(relativeHandlerMiddle / height.toFloat(), false)
                } else {
                    setPosition(event.rawX - currentPos + relativeHandlerMiddle)
                    pdfView!!.setPositionOffset(relativeHandlerMiddle / width.toFloat(), false)
                }
                return true
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                hideDelayed()
                pdfView!!.performPageSnap()
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