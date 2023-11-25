package com.harissk.pdfpreview.listener

import android.view.MotionEvent
import com.harissk.pdfpreview.link.LinkHandler
import com.harissk.pdfpreview.model.LinkTapEvent


/**
 * Created by Harishkumar on 25/11/23.
 */

class Callbacks {
    /**
     * Call back object to call when the PDF is loaded
     */
    private var onLoadCompleteListener: OnLoadCompleteListener? = null

    /**
     * Call back object to call when document loading error occurs
     */
    private var onErrorListener: OnErrorListener? = null

    /**
     * Call back object to call when the page load error occurs
     */
    private var onPageErrorListener: OnPageErrorListener? = null

    /**
     * Call back object to call when the document is initially rendered
     */
    private var onRenderListener: OnRenderListener? = null

    /**
     * Call back object to call when the page has changed
     */
    private var onPageChangeListener: OnPageChangeListener? = null

    /**
     * Call back object to call when the page is scrolled
     */
    private var onPageScrollListener: OnPageScrollListener? = null

    /**
     * Call back object to call when the above layer is to drawn
     */
    private var onDrawListener: OnDrawListener? = null
    private var onDrawAllListener: OnDrawListener? = null

    /**
     * Call back object to call when the user does a tap gesture
     */
    private var onTapListener: OnTapListener? = null

    /**
     * Call back object to call when the user does a long tap gesture
     */
    private var onLongPressListener: OnLongPressListener? = null

    /**
     * Call back object to call when clicking link
     */
    private var linkHandler: LinkHandler? = null
    fun setOnLoadComplete(onLoadCompleteListener: OnLoadCompleteListener?) {
        this.onLoadCompleteListener = onLoadCompleteListener
    }

    fun callOnLoadComplete(pagesCount: Int) {
        onLoadCompleteListener?.loadComplete(pagesCount)
    }

    fun setOnError(onErrorListener: OnErrorListener?) {
        this.onErrorListener = onErrorListener
    }

    fun getOnError(): OnErrorListener? {
        return onErrorListener
    }

    fun setOnPageError(onPageErrorListener: OnPageErrorListener?) {
        this.onPageErrorListener = onPageErrorListener
    }

    fun callOnPageError(page: Int, error: Throwable?): Boolean {
        onPageErrorListener?.onPageError(page, error)
        return onPageErrorListener != null
    }

    fun setOnRender(onRenderListener: OnRenderListener?) {
        this.onRenderListener = onRenderListener
    }

    fun callOnRender(pagesCount: Int) {
        onRenderListener?.onInitiallyRendered(pagesCount)
    }

    fun setOnPageChange(onPageChangeListener: OnPageChangeListener?) {
        this.onPageChangeListener = onPageChangeListener
    }

    fun callOnPageChange(page: Int, pagesCount: Int) {
        onPageChangeListener?.onPageChanged(page, pagesCount)
    }

    fun setOnPageScroll(onPageScrollListener: OnPageScrollListener?) {
        this.onPageScrollListener = onPageScrollListener
    }

    fun callOnPageScroll(currentPage: Int, offset: Float) {
        onPageScrollListener?.onPageScrolled(currentPage, offset)
    }

    fun setOnDraw(onDrawListener: OnDrawListener?) {
        this.onDrawListener = onDrawListener
    }

    fun getOnDraw(): OnDrawListener? = onDrawListener

    fun setOnDrawAll(onDrawAllListener: OnDrawListener?) {
        this.onDrawAllListener = onDrawAllListener
    }

    fun getOnDrawAll(): OnDrawListener? = onDrawAllListener

    fun setOnTap(onTapListener: OnTapListener?) {
        this.onTapListener = onTapListener
    }

    fun callOnTap(event: MotionEvent?): Boolean = onTapListener?.onTap(event) ?: false

    fun setOnLongPress(onLongPressListener: OnLongPressListener?) {
        this.onLongPressListener = onLongPressListener
    }

    fun callOnLongPress(event: MotionEvent?) {
        onLongPressListener?.onLongPress(event)
    }

    fun setLinkHandler(linkHandler: LinkHandler?) {
        this.linkHandler = linkHandler
    }

    fun callLinkHandler(event: LinkTapEvent?) {
        linkHandler?.handleLinkEvent(event)
    }
}