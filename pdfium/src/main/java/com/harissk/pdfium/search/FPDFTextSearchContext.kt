package com.harissk.pdfium.search

/**
 * An abstract class representing the context for searching text within a PDF document.
 *
 * @param pageIndex The index of the page to search.
 * @param query The search query string.
 * @param isMatchCase Whether to match the case of the query.
 * @param isMatchWholeWord Whether to match only whole words.
 */
abstract class FPDFTextSearchContext(
    override val pageIndex: Int,
    override val query: String,
    override val isMatchCase: Boolean,
    override val isMatchWholeWord: Boolean,
) : TextSearchContext {

    var mHasNext: Boolean = true
    var mHasPrev: Boolean = false

    /**
     * The number of search results found.
     */
    override val countResult: Int = 0

    init {
        prepareSearch()
    }

    /**
     * Returns whether there are more results to be found in the current search direction.
     */
    override val hasNext: Boolean
        get() = countResult > 0 || mHasNext

    /**
     * Returns whether there are previous results in the current search direction.
     */
    override val hasPrev: Boolean
        get() = countResult > 0 || mHasPrev

    /**
     * Starts a new search for the query within the specified page.
     */
    override fun startSearch() {
        searchNext
    }

    /**
     * Stops the current search.
     */
    override fun stopSearch() = Unit
}