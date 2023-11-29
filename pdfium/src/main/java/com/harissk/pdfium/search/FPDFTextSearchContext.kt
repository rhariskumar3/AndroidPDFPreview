package com.harissk.pdfium.search

/**
 * Created by Harishkumar on 26/11/23.
 */

abstract class FPDFTextSearchContext(
    override val pageIndex: Int,
    override val query: String,
    override val isMatchCase: Boolean,
    override val isMatchWholeWord: Boolean,
) : TextSearchContext {

    var mHasNext = true
    var mHasPrev = false

    init {
        prepareSearch()
    }

    override val hasNext: Boolean
        get() = countResult > 0 || mHasNext

    override val hasPrev: Boolean
        get() = countResult > 0 || mHasPrev

    override fun startSearch() {
        searchNext
    }

    override fun stopSearch() = Unit
}