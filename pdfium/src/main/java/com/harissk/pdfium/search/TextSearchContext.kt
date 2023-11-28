package com.harissk.pdfium.search

import android.graphics.RectF

/**
 * Created by Harishkumar on 26/11/23.
 */

interface TextSearchContext {
    fun prepareSearch()
    fun getPageIndex(): Int
    fun getQuery(): String?
    fun isMatchCase(): Boolean
    fun isMatchWholeWord(): Boolean
    fun countResult(): Int
    operator fun hasNext(): Boolean
    fun hasPrev(): Boolean
    fun searchNext(): RectF?
    fun searchPrev(): RectF?
    fun startSearch()
    fun stopSearch()
}