package com.harissk.pdfium.search

import android.graphics.RectF

/**
 * Created by Harishkumar on 26/11/23.
 */

interface TextSearchContext {
    val pageIndex: Int
    val query: String?
    val isMatchCase: Boolean
    val isMatchWholeWord: Boolean
    val countResult: Int
    val hasNext: Boolean
    val hasPrev: Boolean
    val searchNext: RectF?
    val searchPrev: RectF?
    fun prepareSearch()
    fun startSearch()
    fun stopSearch()
}