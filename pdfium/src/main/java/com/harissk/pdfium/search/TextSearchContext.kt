package com.harissk.pdfium.search

import android.graphics.RectF

/**
 * An interface representing the context for searching text within a PDF document.
 */
interface TextSearchContext {

    /**
     * The index of the page to search.
     */
    val pageIndex: Int

    /**
     * The search query string.
     */
    val query: String?

    /**
     * Whether to match the case of the query.
     */
    val isMatchCase: Boolean

    /**
     * Whether to match only whole words.
     */
    val isMatchWholeWord: Boolean

    /**
     * The number of search results found.
     */
    val countResult: Int

    /**
     * Returns whether there are more results to be found in the current search direction.
     */
    val hasNext: Boolean

    /**
     * Returns whether there are previous results in the current search direction.
     */
    val hasPrev: Boolean

    /**
     * The bounding rectangle of the next search result.
     */
    val searchNext: RectF?

    /**
     * The bounding rectangle of the previous search result.
     */
    val searchPrev: RectF?

    /**
     * Prepares the search by initializing the search engine and setting initial search flags.
     */
    fun prepareSearch()

    /**
     * Starts a new search for the query within the specified page.
     */
    fun startSearch()

    /**
     * Stops the current search.
     */
    fun stopSearch()
}