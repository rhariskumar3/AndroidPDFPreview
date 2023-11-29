package com.harissk.pdfium

import android.graphics.RectF

/**
 * Created by Harishkumar on 26/11/23.
 */

data class Link(
    val bounds: RectF,
    val destPageIdx: Int? = null,
    val uri: String? = null,
)