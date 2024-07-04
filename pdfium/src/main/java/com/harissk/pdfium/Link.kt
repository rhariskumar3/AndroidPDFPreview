package com.harissk.pdfium

import android.graphics.RectF

/**
 * Represents a link in a PDF document.
 *
 * @param bounds The bounding rectangle of the link.
 * @param destPageIdx The destination page index, if the link is an internal link.
 * @param uri The URI of the link, if the link is an external link.
 */
data class Link(
    val bounds: RectF,
    val destPageIdx: Int? = null,
    val uri: String? = null,
)