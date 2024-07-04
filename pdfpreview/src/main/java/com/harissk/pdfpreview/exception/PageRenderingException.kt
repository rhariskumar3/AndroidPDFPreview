package com.harissk.pdfpreview.exception

/**
 * Exception thrown when an error occurs during page rendering.
 *
 * @param page The page number where the error occurred.
 * @param throwable The underlying exception that caused the error.
 */
class PageRenderingException(val page: Int, private val throwable: Throwable) :
    Exception(throwable) {

    /**
     * Returns a message that describes the exception.
     */
    override val message: String
        get() = "Error rendering page $page: ${throwable.message}"
}