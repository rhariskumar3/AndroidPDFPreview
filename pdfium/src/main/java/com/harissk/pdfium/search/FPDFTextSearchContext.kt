package com.harissk.pdfium.search

/**
 * Created by Harishkumar on 26/11/23.
 */

abstract class FPDFTextSearchContext protected constructor(
    pageIndex: Int,
    query: String,
    matchCase: Boolean,
    matchWholeWord: Boolean,
) :
    TextSearchContext {
    private val pageIndex: Int
    private val query: String
    private val matchCase: Boolean
    private val matchWholeWord: Boolean
    var mHasNext = true
    var mHasPrev = false

    init {
        this.pageIndex = pageIndex
        this.query = query
        this.matchCase = matchCase
        this.matchWholeWord = matchWholeWord
        prepareSearch()
    }

    override fun getPageIndex(): Int {
        return pageIndex
    }

    override fun getQuery(): String {
        return query
    }

    override fun isMatchCase(): Boolean {
        return matchCase
    }

    override fun isMatchWholeWord(): Boolean {
        return matchWholeWord
    }

    override fun hasNext(): Boolean {
        return countResult() > 0 || mHasNext
    }

    override fun hasPrev(): Boolean {
        return countResult() > 0 || mHasPrev
    }

    override fun startSearch() {
        searchNext()
    }

    override fun stopSearch() {}
}